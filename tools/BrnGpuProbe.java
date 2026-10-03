package com.ohinteractive.seedv6.core.brn3;

import static org.jocl.CL.*;
import org.jocl.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.service.BrnResearchData;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;

/** Isolated JOCL experiment, compiled explicitly with JOCL2.0.5, never shipped.
 * Measures an actual BRN minibatch's sparse Adam workload, NOT full GPU training. */
public final class BrnGpuProbe {
    static final String KERNEL="""
        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
        #pragma OPENCL FP_CONTRACT OFF
        __kernel void adam(__global double *w,__global double *m,__global double *v,
            __global const double *g,__global const int *indices,__global double *out,
            const int n,const double c1,const double c2) {
            int t=get_global_id(0);if(t>=n)return;int i=indices[t];double d=g[t]/128.0;
            double a=.9*m[i]+.1*d,b=.999*v[i]+.001*d*d;
            double value=w[i]-.003*(a/c1)/(sqrt(b/c2)+1e-8);
            m[i]=a;v[i]=b;w[i]=value;out[t]=value;
        }
        """;
    static Object field(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    static String info(cl_device_id device,int key) {
        long[] size={0};clGetDeviceInfo(device,key,0,null,size);byte[] b=new byte[(int)size[0]];
        clGetDeviceInfo(device,key,b.length,Pointer.to(b),null);return new String(b).replace("\0","");
    }
    static cl_mem buffer(cl_context context,Object values,long bytes) {
        Pointer p=values instanceof double[] a?Pointer.to(a):Pointer.to((int[])values);
        return clCreateBuffer(context,CL_MEM_READ_WRITE|CL_MEM_COPY_HOST_PTR,bytes,p,null);
    }
    static void write(cl_command_queue q,cl_mem b,double[] values) {
        clEnqueueWriteBuffer(q,b,CL_TRUE,0,(long)values.length*8,Pointer.to(values),0,null,null);
    }
    static void cpu(double[] w,double[] m,double[] v,double[] g,int[] indices,double c1,double c2) {
        for(int t=0;t<indices.length;t++) {
            int i=indices[t];double d=g[t]/128;
            m[i]=.9*m[i]+.1*d;v[i]=.999*v[i]+.001*d*d;
            w[i]-=.003*(m[i]/c1)/(Math.sqrt(v[i]/c2)+1e-8);
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("DATA NEW_REPORT");
        Path output=Path.of(args[1]);if(Files.exists(output))throw new IllegalArgumentException("Use a new report");
        var data=BrnResearchData.read(Path.of(args[0]),false).training();var trainer=new Brn3Trainer(71);
        long[][] boards=new long[128][];double[] targets=new double[128];
        for(int b=0;b<16;b++) {
            for(int i=0;i<128;i++){var e=data.get(b*128+i);boards[i]=e.board();targets[i]=e.outcome();}
            trainer.trainBatch(boards,targets,128);
        }
        int size=(Integer)field(trainer,"size");int[] rows=(int[])field(trainer,"touched");
        int[] indices=new int[size*8+Brn3Layout.MODEL_PARAMETERS-Brn3Layout.DENSE];int pos=0;
        for(int i=Brn3Layout.DENSE;i<Brn3Layout.MODEL_PARAMETERS;i++)indices[pos++]=i;
        for(int r=0;r<size;r++)for(int c=0;c<8;c++)indices[pos++]=rows[r]+c;
        double[] w=((double[])field(trainer,"weights")).clone(),m=((double[])field(trainer,"first")).clone(),v=((double[])field(trainer,"second")).clone();
        double[] dense=(double[])field(trainer,"gradient"),g=new double[indices.length],out=new double[indices.length];
        for(int i=0;i<g.length;i++)g[i]=dense[indices[i]];
        double c1=1-Math.pow(.9,17),c2=1-Math.pow(.999,17);
        setExceptionsEnabled(true);
        int[] count={0};clGetPlatformIDs(0,null,count);cl_platform_id[] platforms=new cl_platform_id[count[0]];clGetPlatformIDs(platforms.length,platforms,null);
        cl_device_id device=null;cl_platform_id platform=null;
        for(var p:platforms) {
            cl_device_id[] devices=new cl_device_id[16];
            try{clGetDeviceIDs(p,CL_DEVICE_TYPE_GPU,devices.length,devices,count);}catch(CLException noGpu){continue;}
            for(int i=0;i<count[0];i++)if(info(devices[i],CL_DEVICE_NAME).contains("4060")){device=devices[i];platform=p;}
        }
        if(device==null)throw new IllegalStateException("Expected inspected RTX4060 not found; no substitution");
        var report=new LinkedHashMap<String,Object>();report.put("device",info(device,CL_DEVICE_NAME));report.put("driver",info(device,CL_DRIVER_VERSION));
        report.put("deviceVersion",info(device,CL_DEVICE_VERSION));report.put("extensions",info(device,CL_DEVICE_EXTENSIONS));
        report.put("parameters",w.length);report.put("touchedCoordinates",indices.length);report.put("batch",128);report.put("sourceBatches",16);
        var properties=new cl_context_properties();properties.addProperty(CL_CONTEXT_PLATFORM,platform);
        cl_context context=clCreateContext(properties,1,new cl_device_id[]{device},null,null,null);
        cl_command_queue queue=clCreateCommandQueue(context,device,0,null);
        cl_program program=clCreateProgramWithSource(context,1,new String[]{KERNEL},null,null);
        long compilation=System.nanoTime();clBuildProgram(program,0,null,"",null,null);report.put("compileSeconds",(System.nanoTime()-compilation)/1e9);
        cl_kernel kernel=clCreateKernel(program,"adam",null);
        cl_mem[] buffers={buffer(context,w,w.length*8L),buffer(context,m,m.length*8L),buffer(context,v,v.length*8L),
                buffer(context,g,g.length*8L),buffer(context,indices,indices.length*4L),buffer(context,out,out.length*8L)};
        try {
            for(int i=0;i<buffers.length;i++)clSetKernelArg(kernel,i,Sizeof.cl_mem,Pointer.to(buffers[i]));
            clSetKernelArg(kernel,6,Sizeof.cl_int,Pointer.to(new int[]{indices.length}));
            clSetKernelArg(kernel,7,Sizeof.cl_double,Pointer.to(new double[]{c1}));clSetKernelArg(kernel,8,Sizeof.cl_double,Pointer.to(new double[]{c2}));
            long[] global={(indices.length+255L)/256*256},local={256};
            clEnqueueNDRangeKernel(queue,kernel,1,null,global,local,0,null,null);
            clEnqueueReadBuffer(queue,buffers[5],CL_TRUE,0,out.length*8L,Pointer.to(out),0,null,null);
            double[] expected=w.clone(),em=m.clone(),ev=v.clone();cpu(expected,em,ev,g,indices,c1,c2);
            double max=0;for(int i=0;i<out.length;i++)max=Math.max(max,Math.abs(out[i]-expected[indices[i]]));
            report.put("maxOneUpdateAbsoluteWeightError",max);
            if(!Double.isFinite(max)||max>1e-12)throw new AssertionError("GPU one-update parity: "+max);
            var measurements=new ArrayList<Map<String,Object>>();report.put("trials",measurements);
            for(int trial=-1;trial<3;trial++)for(boolean transfer:new boolean[]{false,true}) {
                write(queue,buffers[0],w);write(queue,buffers[1],m);write(queue,buffers[2],v);
                int iterations=64;long begin=System.nanoTime();
                for(int i=0;i<iterations;i++) {
                    if(transfer){write(queue,buffers[3],g);clEnqueueWriteBuffer(queue,buffers[4],CL_TRUE,0,indices.length*4L,Pointer.to(indices),0,null,null);}
                    clEnqueueNDRangeKernel(queue,kernel,1,null,global,local,0,null,null);
                    if(transfer)clEnqueueReadBuffer(queue,buffers[5],CL_TRUE,0,out.length*8L,Pointer.to(out),0,null,null);
                    else clFinish(queue);
                }
                double gpu=(System.nanoTime()-begin)/1e6/iterations;
                expected=w.clone();em=m.clone();ev=v.clone();begin=System.nanoTime();
                for(int i=0;i<iterations;i++)cpu(expected,em,ev,g,indices,c1,c2);
                double cpu=(System.nanoTime()-begin)/1e6/iterations;
                expected=w.clone();em=m.clone();ev=v.clone();begin=System.nanoTime();
                for(int iteration=0;iteration<iterations;iteration++) {
                    Brn3VectorKernels.update(expected,em,ev,dense,Brn3Layout.DENSE,Brn3Layout.MODEL_PARAMETERS,128,c1,c2,.1,.001,trainer.config());
                    for(int r=0;r<size;r++)Brn3VectorKernels.update(expected,em,ev,dense,rows[r],rows[r]+8,128,c1,c2,.1,.001,trainer.config());
                }
                double simd=(System.nanoTime()-begin)/1e6/iterations;
                measurements.add(Map.of("trial",trial,"transfer",transfer,"gpuMsPerUpdate",gpu,"scalarCpuMsPerUpdate",cpu,"simdCpuMsPerUpdate",simd));
            }
            report.put("limits","Same real gradient/touched set replayed; GPU moments resident; includes synchronous submission. No forward/backward or packing cost, no full trainer claim. SIMD CPU uses integrated update kernel.");
            String json=com.ohinteractive.seedv6.training.data.DataFiles.JSON.toJson(report);
            Files.writeString(output,json);System.out.println(json);
        } finally {
            for(var b:buffers)clReleaseMemObject(b);clReleaseKernel(kernel);clReleaseProgram(program);clReleaseCommandQueue(queue);clReleaseContext(context);
        }
    }
}
