import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class Test {

    public static void main(String[] args) {
        testTree();
    }

    public static void testTree() {
        RBTreeMap<Integer, Integer> map = new RBTreeMap<>();
        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        BPlusTreeMap<Integer, Integer> bPlusTreeMap = new BPlusTreeMap<>(100);
        List<Integer> data = new ArrayList<>();
        int testNumber = 2000000;
        for (int i = 0; i < testNumber; i++) {
            data.add(i);
        }
        System.out.println("-------------------------------性能测试：" + testNumber + "个元素随机插入，查询，删除----------------------------------" );
        // -------------------------------新增
        Collections.shuffle(data);
        long startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            treeMap.put(datum, datum - 1);
        }
        System.out.println("jdk红黑树插入用时:" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            map.put(datum, datum - 1);
        }
        System.out.println("手写红黑插入用时：" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            bPlusTreeMap.put(datum, datum - 1);
        }
        System.out.println("手写b+插入用时：" + (System.currentTimeMillis() - startTime));
        // ---------------------------------查询
        Collections.shuffle(data);
        startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            treeMap.get(datum);
        }
        System.out.println("jdk红黑树查询用时:" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            map.get(datum);
        }
        System.out.println("手写红黑查询用时：" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (Integer datum : data) {
            bPlusTreeMap.get(datum);
        }
        System.out.println("手写b+查询用时：" + (System.currentTimeMillis() - startTime));
        //----------------------------------删除
        Collections.shuffle(data);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < testNumber - 20; i++) {
            treeMap.remove(data.get(i));
        }
        System.out.println("jdk红黑删除用时：" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (int i = 0; i < testNumber - 20; i++) {
            map.remove(data.get(i));
        }
        System.out.println("手写红黑删除用时：" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        for (int i = 0; i < testNumber - 20; i++) {
            bPlusTreeMap.remove(data.get(i));
        }
        System.out.println("手写b+删除用时：" + (System.currentTimeMillis() - startTime));
        System.out.println(treeMap.keySet());
        System.out.println(map.keySet());
        System.out.println(bPlusTreeMap.keySet());
    }
}
