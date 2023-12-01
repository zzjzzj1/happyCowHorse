import java.util.*;

public class SuffixArrayGetter {
    private enum SuffixType {
        S,
        L;
    }

    private static class Bucket {
        int startIndex;
        int number;
        int currentLeft;
        int currentRight;
    }

    // contains left and right
    private static class LMS {
        int left;
        int right;
        int number;

        public LMS(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "LMS{" +
                    "left=" + left +
                    ", right=" + right +
                    ", number=" + number +
                    '}';
        }
    }

    private int[] getSuffixArray(Integer[] line) {
        int[] sa = new int[line.length + 1];
        sa[0] = line.length;
        TreeMap<Integer, Bucket> bucket = getBucket(line);
        // 判断是否可以根据首自己排序
        if (judgeCanSortByBucket(bucket, line, sa)) {
            return sa;
        }
        // 获取后缀类型
        SuffixType[] suffixType = getSuffixType(line);
        List<LMS> lmsList = getLMSList(suffixType);
        sortLMS(lmsList, sa, bucket, line, suffixType);
        Integer[] temp = new Integer[lmsList.size()];
        for (int i = 0; i < lmsList.size(); i++) {
            temp[i] = lmsList.get(i).number;
        }
        int[] sa_1 = getSuffixArray(temp);
        inducedSort(suffixType, lmsList, sa, sa_1, bucket, line);
        return sa;
    }

    private void inducedSort(SuffixType[] suffixType, List<LMS> lmsList, int[] sa, int[] sa_1, TreeMap<Integer, Bucket> bucket, Integer[] line) {
        for (int i = 1; i < sa.length; i++) {
            sa[i] = -1;
        }
        for (Bucket value : bucket.values()) {
            value.currentRight = value.startIndex + value.number - 1;
        }
        for (int i = sa_1.length - 1; i >= 1; i--) {
            LMS lms = lmsList.get(sa_1[i]);
            if (lms.left < line.length) {
                sa[bucket.get(line[lms.left]).currentRight--] = lms.left;
            }
        }
        inducedSortTemp(suffixType, sa, bucket, line);
    }

    private void inducedSortTemp(SuffixType[] suffixType, int[] sa, TreeMap<Integer, Bucket> bucket, Integer[] line) {
        for (Bucket value : bucket.values()) {
            value.currentLeft = value.startIndex;
            value.currentRight = value.startIndex + value.number - 1;
        }
        // 诱导生成L
        for (int i = 0; i < sa.length; i++) {
            int suffix = sa[i] - 1;
            if (sa[i] > 0 && suffixType[suffix] == SuffixType.L) {
                sa[bucket.get(line[suffix]).currentLeft++] = suffix;
            }
        }
        // 诱导生成S
        for (int i = sa.length - 1; i >= 0; i--) {
            int suffix = sa[i] - 1;
            if (sa[i] > 0 && suffixType[suffix] == SuffixType.S) {
                sa[bucket.get(line[suffix]).currentRight--] = suffix;
            }
        }
    }

    private Map<Integer, LMS> getLmsMap(List<LMS> lmsList) {
        Map<Integer, LMS> record = new HashMap<>();
        for (LMS lms : lmsList) {
            record.put(lms.left, lms);
        }
        return record;
    }

    // 排序lms
    private void sortLMS(List<LMS> lmsList, int[] sa, TreeMap<Integer, Bucket> bucket, Integer[] line, SuffixType[] suffixType) {
        int[] tempSa_1 = new int[lmsList.size() + 1];
        for (int i = 1; i < lmsList.size() + 1; i++) {
            tempSa_1[i] = i - 1;
        }
        Map<Integer, LMS> lmsMap = getLmsMap(lmsList);
        inducedSort(suffixType, lmsList, sa, tempSa_1, bucket, line);
        int number = 0;
        LMS last = null;
        for (int index : sa) {
            if (lmsMap.containsKey(index)) {
                LMS lms = lmsMap.get(index);
                lms.number = number++;
                // 相同名称lms修正
                if (judgeSame(last, lms, line)) {
                    lms.number --;
                    number --;
                }
                last = lms;
            }
        }
    }

    private boolean judgeSame(LMS a, LMS b, Integer[] line) {
        if (a == null || b == null) {
            return false;
        }
        if (a.right - a.left != b.right - b.left) {
            return false;
        }
        if (a.right == line.length || b.right == line.length) {
            return false;
        }
        for (int i = 0; a.left + i <= a.right; i++) {
            if (!Objects.equals(line[a.left + i], line[b.left + i])) {
                return false;
            }
        }
        return true;
    }

    private List<LMS> getLMSList(SuffixType[] suffixType) {
        List<LMS> data = new ArrayList<>();
        Integer left = null;
        for (int i = 0; i < suffixType.length; i++) {
            if (judgeLMS(i, suffixType)) {
                if (left != null) {
                    LMS lms = new LMS(left, i);
                    data.add(lms);
                }
                left = i;
            }
        }
        data.add(new LMS(suffixType.length - 1, suffixType.length - 1));
        return data;
    }

    private boolean judgeLMS(int i, SuffixType[] suffixType) {
        return i != 0 && suffixType[i] == SuffixType.S && suffixType[i - 1] == SuffixType.L;
    }

    private boolean judgeCanSortByBucket(TreeMap<Integer, Bucket> bucket, Integer[] line, int[] ans) {
        if (bucket.size() == line.length) {
            for (int i = 0; i < line.length; i++) {
                ans[bucket.get(line[i]).startIndex] = i;
            }
            return true;
        }
        return false;
    }

    private TreeMap<Integer, Bucket> getBucket(Integer[] line) {
        TreeMap<Integer, Bucket> bucket = new TreeMap<>();
        for (Integer item : line) {
            if (!bucket.containsKey(item)) {
                bucket.put(item, new Bucket());
            }
            bucket.get(item).number++;
        }
        int index = 1;
        for (Map.Entry<Integer, Bucket> entry : bucket.entrySet()) {
            Bucket value = entry.getValue();
            value.startIndex = index;
            index += value.number;
        }
        return bucket;
    }

    // 动态规划思路获取后缀类型
    private SuffixType[] getSuffixType(Integer[] line) {
        SuffixType[] suffixTypeArray = new SuffixType[line.length + 1];
        suffixTypeArray[line.length] = SuffixType.S;
        suffixTypeArray[line.length - 1] = SuffixType.L;
        for (int i = line.length - 2; i >= 0; i--) {
            if (line[i] > line[i + 1]) {
                suffixTypeArray[i] = SuffixType.L;
                continue;
            }
            if (line[i] < line[i + 1]) {
                suffixTypeArray[i] = SuffixType.S;
                continue;
            }
            suffixTypeArray[i] = suffixTypeArray[i + 1];
        }
        return suffixTypeArray;
    }

    public int[] getSuffixArray(String line) {
        Integer[] temp = new Integer[line.length()];
        for (int i = 0; i < line.length(); i++) {
            temp[i] = (int) line.charAt(i);
        }
        return getSuffixArray(temp);
    }

    private static String mockLine(int length) {
        Random random = new Random();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < length; i++) {
            line.append(random.nextInt(10));
        }
        return line.toString();
    }

    public static void main(String[] args) {
        SuffixArrayGetter suffixArrayGetter = new SuffixArrayGetter();
        String line = mockLine(100000);
        long startTime = System.currentTimeMillis();
        int[] suffixArray = suffixArrayGetter.getSuffixArray(line);
        System.out.println(System.currentTimeMillis() - startTime);
        // 验证后缀数组正确性
        String last = null;
        boolean res = true;
        for (int i : suffixArray) {
            String content = line.substring(i);
            if (!check(last, content)) {
                res = false;
            }
            last = content;
        }
        System.out.println(res);
    }

    public static boolean check(String last, String current) {
        if (last == null) {
            return true;
        }
        for (int i = 0; i < Math.min(last.length(), current.length()); i++) {
            if ((int) last.charAt(i) < (int) current.charAt(i)) {
                return true;
            }
            if ((int) last.charAt(i) > (int) current.charAt(i)) {
                return false;
            }
        }
        return current.length() > last.length();
    }

}
