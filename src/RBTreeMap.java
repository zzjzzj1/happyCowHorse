import java.util.*;

public class RBTreeMap<K extends Comparable<K>, V> {
    RBNode<K, V> root;
    List<K> keyList;

    private void setRoot(RBNode<K, V> node) {
        this.root = node;
        this.root.parent = null;
    }

    // 旋转树前置操作，如果cur为根节点，将根节点替换为pivot，如果cur不是根节点，用pivot替换cur的位置
    private void beforeRotate(RBNode<K, V> pivot, RBNode<K, V> cur) {
        if (cur == root) {
            this.setRoot(pivot);
            return;
        }
        if (isParentLeft(cur)) cur.parent.setLeft(pivot);
        else cur.parent.setRight(pivot);
    }

    // 右旋操作
    private void rotateRight(RBNode<K, V> pivot, RBNode<K, V> cur) {
        beforeRotate(pivot, cur);
        RBNode<K, V> tempRight = pivot.right;
        pivot.setRight(cur);
        cur.setLeft(tempRight);
    }

    // 左旋操作
    private void rotateLeft(RBNode<K, V> pivot, RBNode<K, V> cur) {
        beforeRotate(pivot, cur);
        RBNode<K, V> tempLeft = pivot.left;
        pivot.setLeft(cur);
        cur.setRight(tempLeft);
    }

    // 获取节点的祖父节点
    private RBNode<K, V> grandpa(RBNode<K, V> node) {
        if (node.parent == null) return null;
        return node.parent.parent;
    }

    // 判断节点是否为父亲节点的左节点
    private boolean isParentLeft(RBNode<K, V> node) {
        if (node.parent == null) return false;
        return node.parent.left == node;
    }

    // 判断节点是否为父亲节点的右节点
    private boolean isParentRight(RBNode<K, V> node) {
        if (node.parent == null) return false;
        return node.parent.right == node;
    }

    // 获取节点的兄弟节点
    private RBNode<K, V> brother(RBNode<K, V> node) {
        if (node.parent == null) return null;
        if (isParentLeft(node)) return node.parent.right;
        return node.parent.left;
    }

    // 获取节点的叔叔节点
    private RBNode<K, V> uncle(RBNode<K, V> node) {
        if (node.parent == null) return null;
        return brother(node.parent);
    }

    // 获取节点
    public V get(K key) {
        RBNode<K, V> node = findNode(key);
        if (node != null) return node.value;
        return null;
    }

    // 添加节点
    public V put(K key, V value) {
        // 如果节点未空，新增一个节点
        if (root == null) {
            this.setRoot(new RBNode<>(key, value, false));
            return null;
        }
        return putVal(key, value);
    }

    // 删除节点
    public V remove(K key) {
        if (root == null) return null;
        return removeKey(key);
    }

    // 搜索节点
    private RBNode<K, V> findNode(K key) {
        if (root == null) return null;
        RBNode<K, V> cur = root;
        while (cur != null) {
            if (cur.key.compareTo(key) == 0) return cur;
            if (cur.key.compareTo(key) < 0) cur = cur.right;
            else cur = cur.left;
        }
        return null;
    }

    // 删除节点
    private V removeKey(K key) {
        // 向下寻找目标节点
        RBNode<K, V> waitDelete = findNode(key);
        // 如果树中没有该节点，则直接返回
        if (waitDelete == null) return null;
        V tempValue = waitDelete.value;
        // 获取节点儿子数量
        int sonNumber = waitDelete.getSonNumber();
        /*
            如果为红色节点且没有儿子，则直接删除
            注意：不可能出现红色节点只有一个儿子的情况！！！
         */
        if (waitDelete.isRed() && sonNumber == 0) {
            deleteRedNoSon(waitDelete);
            return tempValue;
        }
        // 如果为黑色节点且只有一个儿子，直接删除该节点，并用节点子节点代替该节点的位置
        if (waitDelete.isBlack() && sonNumber == 1) {
            deleteBlackOneSon(waitDelete);
            return tempValue;
        }
        // 如果有两个儿子，执行相应逻辑
        if (sonNumber == 2) {
            deleteTwoSon(waitDelete);
            return tempValue;
        }
        // 如果删除的是黑色节点且没有儿子
        if (waitDelete.isBlack() && sonNumber == 0) {
            deleteBlackNoSon(waitDelete);
            return tempValue;
        }
        return tempValue;
    }

    // 直接删除红色节点（出口情形）
    private void deleteRedNoSon(RBNode<K, V> node) {
        RBNode<K, V> parent = node.parent;
        if (node == parent.left) parent.left = null;
        else parent.right = null;
    }

    // 直接删除节点，并用子结点代替该节点位置（出口情形）
    private void deleteBlackOneSon(RBNode<K, V> node) {
        RBNode<K, V> replaceNode = node.left;
        if (replaceNode == null) replaceNode = node.right;
        replaceNode.setBlack();
        if (node == root) {
            this.setRoot(replaceNode);
            return;
        }
        RBNode<K, V> parent = node.parent;
        if (parent.left == node) parent.setLeft(replaceNode);
        else parent.setRight(replaceNode);
    }

    // 作一些前置处理
    private void deleteBlackNoSon(RBNode<K, V> node) {
        if (node == root) {
            root = null;
            return;
        }
        deleteBlackNoSonTemp(node);
        // 删除节点
        RBNode<K, V> parent = node.parent;
        if (parent.left == node) parent.left = null;
        else parent.right = null;
    }

    private void solveDeleteBlackNoSonTempBrotherRed(
            RBNode<K, V> node, RBNode<K, V> brother, RBNode<K, V> parent) {
        brother.setBlack();
        parent.setRed();
        if (isParentLeft(node)) {
            rotateLeft(brother, parent);
            return;
        }
        rotateRight(brother, parent);
    }

    private boolean judgeBlackNoSonOut(RBNode<K, V> node, RBNode<K, V> brother, RBNode<K, V> parent) {
        // 如果兄弟节点为黑色且兄弟节点又红色儿子，则可以将红色儿子借过来从而达到平衡
        if (isParentLeft(node)) {
            if (brother.right != null && brother.right.isRed()) {
                balanceDeleteRightRight(brother, parent);
                return true;
            }
            if (brother.left != null && brother.left.isRed()) {
                balanceDeleteRightLeft(brother, parent);
                return true;
            }

        } else {
            if (brother.left != null && brother.left.isRed()) {
                balanceDeleteLeftLeft(brother, parent);
                return true;
            }
            if (brother.right != null && brother.right.isRed()) {
                balanceDeleteLeftRight(brother, parent);
                return true;
            }
        }
        // 设x为node节点的原始黑色路径长度
        // 如果没有红色节点，且父亲为红色，则一次染色可以结束（左侧黑色路径长度 x - 1, 右侧 x - 1)
        if (parent.isRed()) {
            parent.setBlack();
            brother.setRed();
            return true;
        }
        return false;
    }


    private void deleteBlackNoSonTemp(RBNode<K, V> node) {
        // 出口情形， 不平衡因子达到根节点，可以忽略该因子
        while (node != root) {
            // 获取节点的兄弟节点
            RBNode<K, V> brother = brother(node);
            // 获取节点的父节点
            RBNode<K, V> parent = node.parent;
            assert brother != null;
            // 如果兄弟节点为红色只需要旋转一次就能让他的兄弟节点变为黑色
            if (brother.isRed()) {
                solveDeleteBlackNoSonTempBrotherRed(node, brother, parent);
                continue;
            }
            if (judgeBlackNoSonOut(node, brother, parent)) {
                return;
            }
            // 如果在本层不能解决平衡因子，上推不平衡因子位置，向上寻求解决方案！！！
            // 染色使得本层平衡得到满足！！！ （左侧 x - 1, 右侧 x - 1)
            node = parent;
            brother.setRed();
        }
    }

    //     设f(n) = 某节点到叶子节点经过黑色的数量
    // 参照下列描述！！！！！
    private void balanceDeleteLeftLeft(RBNode<K, V> brother, RBNode<K, V> parent) {
        brother.red = parent.red;
        parent.setBlack();
        brother.left.setBlack();
        rotateRight(brother, parent);
    }

    private void balanceDeleteLeftRight(RBNode<K, V> brother, RBNode<K, V> parent) {
        brother.right.setBlack();
        brother.setRed();
        RBNode<K, V> record = brother.right;
        rotateLeft(brother.right, brother);
        balanceDeleteLeftLeft(record, parent);
    }

    /**
     *             parent(任意颜色(设为Y))
     * waitDelete(黑)               brother(黑)
     *                                      son(红)
     * fn(waitDelete) = fn(brother) = z
     * 设fn(parent) = x
     * 以brother为支点左旋转后
     *            brother(Y)
     * parent(黑)           son(黑)
     * fn(parent) = fn(son) = z
     * fn(brother) = x
     * 满足约束，出口情形！！！！
     */
    private void balanceDeleteRightRight(RBNode<K, V> brother, RBNode<K, V> parent) {
        brother.red = parent.red;
        parent.setBlack();
        brother.right.setBlack();
        rotateLeft(brother, parent);
    }
    /**
     *             parent(任意颜色(设为Y))
     * waitDelete(黑)               brother(黑)
     *                         son(红)
     * fn(waitDelete) = fn(brother) = z
     * 设fn(parent) = x
     * 以son为支点右旋后
     *            parent(任意颜色(设为Y))
     * parent(黑)                 son(黑)
     *                                   brother(红)
     * 转变为上述情形
     */
    private void balanceDeleteRightLeft(RBNode<K, V> brother, RBNode<K, V> parent) {
        brother.left.setBlack();
        brother.setRed();
        RBNode<K, V> record = brother.left;
        rotateRight(brother.left, brother);
        balanceDeleteRightRight(record, parent);
    }

    /**
     * 待删除节点有两个儿子，向右寻找后置节点，将后置节点值赋值给当前节点，转换成删除叶子节点的情况！！！
     */
    private void deleteTwoSon(RBNode<K, V> node) {
        RBNode<K, V> replaceNode = successor(node);
        node.key = replaceNode.key;
        node.value = replaceNode.value;
        // 如果置换节点为红色，则直接删除！！！(不可能出现红色节点只有一个儿子的情况)
        if (replaceNode.isRed()) {
            deleteRedNoSon(replaceNode);
            return;
        }
        // 如果为黑色，判断儿子数量，执行相应逻辑
        if (replaceNode.right == null) deleteBlackNoSon(replaceNode);
        else deleteBlackOneSon(replaceNode);
    }

    // 寻找后置节点
    private RBNode<K, V> successor(RBNode<K, V> node) {
        RBNode<K, V> res = node.right;
        while (res.left != null) res = res.left;
        return res;
    }

    // 添加节点
    private V putVal(K key, V value) {
        RBNode<K, V> current = root;
        // 按照二叉搜索树的添加过程进行添加，先使用红色节点进行添加
        while (current.key.compareTo(key) != 0) {
            if (current.key.compareTo(key) < 0) {
                if (current.right == null) {
                    current.setRight(new RBNode<>(key, value, true));
                    balanceInsertion(current.right);
                    return null;
                }
                current = current.right;
            } else {
                if (current.left == null) {
                    current.setLeft(new RBNode<>(key, value, true));
                    balanceInsertion(current.left);
                    return null;
                }
                current = current.left;
            }
        }
        V tempValue = current.value;
        current.value = value;
        return tempValue;
    }

    /**
     * 插入后处理约束问题
     */
    private void balanceInsertion(RBNode<K, V> cur) {
        RBNode<K, V> parent;
        // 如果不平衡因子到达根节点
        while (cur != null) {
            RBNode<K, V> nextStep;
            parent = cur.parent;
            // 此处只讨论左侧情况
            if (isParentLeft(parent)) {
                if (isParentLeft(cur)) nextStep = balanceLeftLeft(cur, parent);
                else nextStep = balanceLeftRight(cur, parent);
            } else {
                if (isParentRight(cur)) nextStep = balanceRightRight(cur, parent);
                else nextStep = balanceRightLeft(cur, parent);
            }
            if (nextStep == root) {
                assert nextStep != null;
                nextStep.setBlack();
                return;
            }
            cur = nextStep;
        }
    }

    private RBNode<K, V> balanceRightRight(RBNode<K, V> cur, RBNode<K, V> parent) {
        return balanceInsertionTemp(cur, parent, false);
    }

    private RBNode<K, V> balanceRightLeft(RBNode<K, V> cur, RBNode<K, V> parent) {
        if (cur.isRed() && parent.isRed()) {
            rotateRight(cur, parent);
            return balanceRightRight(parent, cur);
        }
        return null;
    }

    /**
     * cur作为支点左旋转
                grandpa(黑)                                           grandpa(黑）
     parent(红）                 uncle()    ------>              cur(红）            uncle(转变成左左形)
                cur(红)                             parent(红)
     */
    private RBNode<K, V> balanceLeftRight(RBNode<K, V> cur, RBNode<K, V> parent) {
        if (cur.isRed() && parent.isRed()) {
            rotateLeft(cur, parent);
            return balanceLeftLeft(parent, cur);
        }
        return null;
    }


    /**
     * 如果uncle为红色节点
     *                   grandpa(黑)                                             grandpa(红)
     *         parent(红)             uncle(红)    ----->               parent(黑）           uncle(黑） (上移不平衡因子)
     *  cur(红）                                                 cur(红)
     *  ------------------------------------------------------------------------------------------------------------
     *  ------------------------------------------------------------------------------------------------------------
     *  ------------------------------------------------------------------------------------------------------------
     *  如果uncle为黑
     *                 grandpa(黑)                                               parent(黑）
     *        parent(红)                 uncle(黑） ----->                  cur(红)              grandpa(红)
     *  cur(红)                                                                                          uncle(黑)
     *  左右两侧黑色节点数量都未改变(满足出口情形 ！！！！)
     */
    private RBNode<K, V> balanceInsertionTemp(RBNode<K, V> cur, RBNode<K, V> parent, boolean rotateRight) {
        if (cur.isRed() && parent.isRed()) {
            RBNode<K, V> uncle = uncle(cur);
            RBNode<K, V> grandpa = grandpa(cur);
            if (uncle == null || uncle.isBlack()) {
                if (rotateRight) rotateRight(parent, grandpa);
                else rotateLeft(parent, grandpa);
                parent.setBlack();
                assert grandpa != null;
                grandpa.setRed();
                return null;
            } else {
                uncle.setBlack();
                parent.setBlack();
                assert grandpa != null;
                grandpa.setRed();
                return grandpa;
            }
        }
        return null;
    }

    private RBNode<K, V> balanceLeftLeft(RBNode<K, V> cur, RBNode<K, V> parent) {
        return balanceInsertionTemp(cur, parent, true);
    }


    public List<K> keySet() {
        keyList = new ArrayList<>();
        midOrder(root);
        return keyList;
    }

    private void midOrder(RBNode<K, V> node) {
        if (node == null) return;
        midOrder(node.left);
        keyList.add(node.key);
        midOrder(node.right);
    }

    public static void main(String[] args) {
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

    static class RBNode<K extends Comparable<K>, V> implements Map.Entry<K, V> {
        RBNode<K, V> parent;
        RBNode<K, V> left;
        RBNode<K, V> right;
        K key;
        V value;
        boolean red;

        public RBNode(K key, V value, boolean red) {
            this.key = key;
            this.value = value;
            this.red = red;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V temp = this.value;
            this.value = value;
            return temp;
        }

        public boolean isRed() {
            return this.red;
        }

        public boolean isBlack() {
            return !this.red;
        }


        public void setRed() {
            this.red = true;
        }

        public void setBlack() {
            this.red = false;
        }

        public void setLeft(RBNode<K, V> node) {
            this.left = node;
            if (node != null)
                node.parent = this;
        }

        public void setRight(RBNode<K, V> node) {
            this.right = node;
            if (node != null)
                node.parent = this;
        }

        public int getSonNumber() {
            int res = 0;
            if (this.left != null) res++;
            if (this.right != null) res++;
            return res;
        }
    }

}




