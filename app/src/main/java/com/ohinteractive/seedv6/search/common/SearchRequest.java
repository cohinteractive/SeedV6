package com.ohinteractive.seedv6.search.common;

public class SearchRequest {
    
    public long[] board;
    public int depth;

    public SearchObserver observer = SearchObserver.NONE;

}
