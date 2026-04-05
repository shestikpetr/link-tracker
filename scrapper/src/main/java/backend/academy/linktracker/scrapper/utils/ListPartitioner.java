package backend.academy.linktracker.scrapper.utils;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ListPartitioner {

    public <T> List<List<T>> partition(List<T> list, int partitionCount) {
        int chunkSize = (list.size() + partitionCount - 1) / partitionCount;
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            result.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return result;
    }
}
