/** Preserve a complete warmup pass, then measure in the same JVM. */
public final class WarmedCost {
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("probe-class roots network");
        var main=Class.forName(args[0]).getMethod("main",String[].class);
        String[] supplied={args[1],args[2]};
        System.out.println("{\"stage\":\"warmup\"}");main.invoke(null,(Object)supplied);
        System.out.println("{\"stage\":\"measured\"}");main.invoke(null,(Object)supplied);
    }
}
