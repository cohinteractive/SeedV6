package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;

/** Independent nominal replays of the prospectively selected integrated predictions. */
public final class Cp64FlatNullAudit {
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("model corpus observations NEW-output");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256)));out.newLine();
            for(String line:Files.readAllLines(Path.of(args[2]))) {
                var row=JsonParser.parseString(line).getAsJsonObject();
                if(!row.has("type") || !row.get("type").getAsString().equals("observation"))continue;
                String id=row.get("id").getAsString(),profile=row.get("profile").getAsString();
                var root=roots.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
                for(var element:row.getAsJsonObject("opportunities").getAsJsonArray("nullSamples")) {
                    var e=element.getAsJsonObject();
                    if(e.get("event").getAsLong()!=1 || !e.get("selectedCutoff").getAsBoolean())continue;
                    var s=gson.fromJson(e.get("sample"),Cp64SelectivityLab.Sample.class);var c=Cp64SelectivityProbe.context(root,s);
                    var r=Cp64SelectivityProbe.reference(model,c.board(),c.history(),s.depth(),s.beta()-1,s.beta(),5000000,false);
                    out.write(gson.toJson(Map.of("type","cutoff-audit","id",id,"profile",profile,"sample",s,"nominal",r)));
                    out.newLine();out.flush();count++;
                }
            }
            if(count==0)throw new AssertionError("No selected cutoff");
            out.write(gson.toJson(Map.of("status","passed","samples",count)));out.newLine();
        }
    }
    private Cp64FlatNullAudit() {}
}
