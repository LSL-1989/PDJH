package com.gocent.pdjh.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RouteCalculationService {

    /**
     * Topologically sorts pending exams based on physiological constraints (e.g., fasting before drinking).
     * @param pendingItems List of exam item codes
     * @param dependencies Map of itemCode to its preRequisiteItemCode
     * @return An ordered list of exam items indicating the optimal path
     */
    public List<String> calculateOptimalPath(List<String> pendingItems, Map<String, String> dependencies) {
        // Build graph
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (String item : pendingItems) {
            graph.putIfAbsent(item, new ArrayList<>());
            inDegree.putIfAbsent(item, 0);
        }

        for (String item : pendingItems) {
            String preReq = dependencies.get(item);
            if (preReq != null && pendingItems.contains(preReq)) {
                graph.get(preReq).add(item);
                inDegree.put(item, inDegree.getOrDefault(item, 0) + 1);
            }
        }

        // Kahn's algorithm for topological sorting
        Queue<String> queue = new LinkedList<>();
        for (String item : pendingItems) {
            if (inDegree.get(item) == 0) {
                queue.add(item);
            }
        }

        List<String> optimalPath = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            optimalPath.add(current);

            for (String neighbor : graph.getOrDefault(current, new ArrayList<>())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // If cycles exist or remaining, just append them
        for (String item : pendingItems) {
            if (!optimalPath.contains(item)) {
                optimalPath.add(item);
            }
        }

        return optimalPath;
    }
}
