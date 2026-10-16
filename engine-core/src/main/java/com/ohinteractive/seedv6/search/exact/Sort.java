package com.ohinteractive.seedv6.search.exact;

/** SR-017 primitive mechanics for at most 512 moves; higher unique keys lead.
 * The key includes generation ordinal, so physical stability is unnecessary.
 * Caller owns all storage. No allocation or object/comparator dispatch. */
final class Sort {
    private Sort() {}

    /** Median-of-three quicksort, insertion leaves, recurse on the smaller partition.
     * The larger partition is iterated, bounding Java stack use by log2(count). */
    static void full(long[] moves, int[] keys, int from, int end, int crossover) {
        while(end - from > crossover) {
            int a = keys[from], b = keys[(from + end) >>> 1], c = keys[end - 1];
            int pivot = a < b ? (b < c ? b : Math.max(a, c)) : (a < c ? a : Math.max(b, c));
            int left = from, right = end - 1;
            while(left <= right) {
                while(keys[left] > pivot) left++;
                while(keys[right] < pivot) right--;
                if(left <= right) {
                    long move = moves[left]; moves[left] = moves[right]; moves[right] = move;
                    int key = keys[left]; keys[left] = keys[right]; keys[right] = key;
                    left++; right--;
                }
            }
            if(right + 1 - from < end - left) {
                full(moves, keys, from, right + 1, crossover);
                from = left;
            } else {
                full(moves, keys, left, end, crossover);
                end = right + 1;
            }
        }
        for(int i = from + 1; i < end; i++) {
            long move = moves[i]; int key = keys[i]; int j = i;
            while(j > from && keys[j - 1] < key) {
                moves[j] = moves[j - 1]; keys[j] = keys[j - 1]; j--;
            }
            moves[j] = move; keys[j] = key;
        }
    }

    /** Preserve the searched prefix; touch only the next move and its selected slot. */
    static void next(long[] moves, int[] keys, int base, int next, int count) {
        int best = next, key = keys[base + next];
        for(int i = next + 1; i < count; i++) {
            if(keys[base + i] > key) { best = i; key = keys[base + i]; }
        }
        if(best != next) {
            long move = moves[next]; moves[next] = moves[best]; moves[best] = move;
            keys[base + best] = keys[base + next]; keys[base + next] = key;
        }
    }
}
