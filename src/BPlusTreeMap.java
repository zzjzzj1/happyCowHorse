import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

class BPlusTreeMap<K extends Comparable<K>, V> {

    NodeGroup<K, V> root;
    NodeGroup<K, V> head;
    int order;
    int halfOrder;

    public BPlusTreeMap(int order) {
        this.order = order;
        this.halfOrder = (this.order + 1) / 2;
        root = new NodeGroup<>(order, true);
        this.head = root;
    }

    public V put(K key, V value) {
        return putVal(key, value);
    }

    public V remove(K key) {
        return removeKey(key);
    }

    public V get(K key) {
        return getVal(key);
    }

    private V removeKey(K key) {
        SearchRes<K, V> search = search(key);
        Node<K, V> temp = getValTemp(search, key);
        if (temp == null) {
            return null;
        }
        V value = ((Leaf<K, V>) temp).value;
        solveDelete(search.nodeGroup, search.position);
        return value;
    }

    private void solveDelete(NodeGroup<K, V> nodeGroup, int position) {
        fixAfterDeleteIndexNode(nodeGroup, position);
        nodeGroup.nodeList.remove(position);
        NodeGroup<K, V> cur = nodeGroup;
        while (cur != root && cur.nodeList.size() < halfOrder) {
            NodeGroup<K, V> left = cur;
            NodeGroup<K, V> right = cur.right;
            if (right == null) {
                left = cur.left;
                right = cur;
            }
            if (left.nodeList.size() + right.nodeList.size() <= order) {
                solveMerge(left, right);
                cur = left.parent;
                int deletePos = cur.searchInsertPosition(left.lastNode().key);
                fixAfterDeleteIndexNode(cur, deletePos);
                cur.nodeList.remove(deletePos);
                continue;
            }
            if (left == cur) {
                getNodeFromRight(cur);
                break;
            }
            getNodeFromLeft(cur);
            break;
        }
        if (cur == root && cur.nodeList.isEmpty() && cur.next != null) {
            this.root = cur.next;
            this.root.parent = null;
        }
    }


    private void getNodeFromLeft(NodeGroup<K, V> current) {
        NodeGroup<K, V> left = current.left;
        K lastKey = left.lastNode().key;
        int number = halfOrder - current.nodeList.size();
        List<Node<K, V>> currentRes = new ArrayList<>();
        for (int i = left.nodeList.size() - number; i < left.nodeList.size(); i++) {
            Node<K, V> node = left.nodeList.get(i);
            currentRes.add(node);
            if (node.next != null) {
                node.next.parent = current;
            }
        }
        currentRes.addAll(current.nodeList);
        current.nodeList = currentRes;
        left.nodeList = left.nodeList.subList(0, left.nodeList.size() - number);
        K replaceKey = left.lastNode().key;
        solveLastIndexChange(left, left.nodeList.size() - 1, lastKey, replaceKey);
    }

    private void getNodeFromRight(NodeGroup<K, V> current) {
        NodeGroup<K, V> right = current.right;
        K lastKey = current.lastNode().key;
        int number = halfOrder - current.nodeList.size();
        List<Node<K, V>> currentRes = new ArrayList<>(current.nodeList);
        for (int i = 0; i < number; i++) {
            Node<K, V> node = right.nodeList.get(i);
            currentRes.add(node);
            if (node.next != null) {
                node.next.parent = current;
            }
        }
        current.nodeList = currentRes;
        right.nodeList = right.nodeList.subList(number, right.nodeList.size());
        K replaceKey = current.lastNode().key;
        solveLastIndexChange(current, current.nodeList.size() - 1, lastKey, replaceKey);
    }

    private void solveLastIndexChange(NodeGroup<K, V> nodeGroup, int position, K lastKey, K replaceKey) {
        while (nodeGroup != root && position == nodeGroup.nodeList.size() - 1 && nodeGroup.right != null) {
            NodeGroup<K, V> parent = nodeGroup.parent;
            int i = parent.searchInsertPosition(lastKey);
            parent.nodeList.get(i).key = replaceKey;
            nodeGroup = parent;
            position = i;
        }
    }

    private void solveMerge(NodeGroup<K, V> left, NodeGroup<K, V> right) {
        List<Node<K, V>> temp = new ArrayList<>();
        temp.addAll(left.nodeList);
        temp.addAll(right.nodeList);
        right.nodeList = temp;
        for (Node<K, V> node : left.nodeList) {
            if (node.next != null) {
                node.next.parent = right;
            }
        }
        right.left = left.left;
        if (left == head) {
            this.head = right;
            return;
        }
        if (left.left != null) {
            left.left.right = right;
        }
    }

    // 删除索引节点
    private void fixAfterDeleteIndexNode(NodeGroup<K, V> nodeGroup, int position) {
        if (nodeGroup == root) {
            return ;
        }
        K key = nodeGroup.nodeList.get(position).key;
        K replaceKey = nodeGroup.nodeList.get(nodeGroup.nodeList.size() - 2).key;
        solveLastIndexChange(nodeGroup, position, key, replaceKey);
    }

    private V getVal(K key) {
        SearchRes<K, V> search = search(key);
        Node<K, V> temp = getValTemp(search, key);
        if (temp == null) {
            return null;
        }
        return ((Leaf<K, V>) temp).value;
    }

    private Node<K, V> getValTemp(SearchRes<K, V> search, K key) {
        int position = search.position;
        NodeGroup<K, V> nodeGroup = search.nodeGroup;
        if (position >= nodeGroup.nodeList.size()) {
            return null;
        }
        if (nodeGroup.nodeList.get(position).key.compareTo(key) == 0) {
            return nodeGroup.nodeList.get(position);
        }
        return null;
    }

    private V putVal(K key, V value) {
        Leaf<K, V> node = new Leaf<>(key, value);
        SearchRes<K, V> searchRes = search(key);
        V returnValue = insertNode(node, searchRes.nodeGroup, searchRes.position);
        this.fixAfterInsertion(searchRes.nodeGroup);
        return returnValue;
    }

    private SearchRes<K, V> search(K key) {
        NodeGroup<K, V> cur = root;
        int insertPosition;
        while (true) {
            insertPosition = cur.searchInsertPosition(key);
            if (cur.bottom) {
                break;
            }
            if (insertPosition >= cur.nodeList.size()) {
                cur = cur.next;
                continue;
            }
            cur = cur.nodeList.get(insertPosition).next;
        }
        return new SearchRes<>(insertPosition, cur);
    }

    private static class SearchRes<K extends Comparable<K>, V> {
        int position;
        NodeGroup<K, V> nodeGroup;

        public SearchRes(int position, NodeGroup<K, V> nodeGroup) {
            this.position = position;
            this.nodeGroup = nodeGroup;
        }
    }

    private V insertNode(Node<K, V> node, NodeGroup<K, V> group, int insertPosition) {
        // 直接加入节点
        if (insertPosition >= group.nodeList.size()) {
            group.nodeList.add(node);
            return null;
        }
        // 替换节点
        if (group.nodeList.get(insertPosition).key.compareTo(node.key) == 0) {
            V temp = ((Leaf<K, V>) group.nodeList.get(insertPosition)).value;
            group.nodeList.set(insertPosition, node);
            return temp;
        }
        // 插入节点
        group.nodeList.add(insertPosition, node);
        return null;
    }

    private List<Node<K, V>> getSplitNodeListLeft(NodeGroup<K, V> cur) {
        List<Node<K, V>> leftList = new ArrayList<>();
        List<Node<K, V>> rightList = new ArrayList<>();
        for (int i = 0; i < halfOrder; i++) {
            leftList.add(cur.nodeList.get(i));
        }
        for (int i = halfOrder; i < cur.nodeList.size(); i++) {
            rightList.add(cur.nodeList.get(i));
        }
        cur.nodeList = rightList;
        return leftList;
    }

    private void fixAfterInsertion(NodeGroup<K, V> nodeGroup) {
        NodeGroup<K, V> cur = nodeGroup;
        while (cur.nodeList.size() > this.order) {
            List<Node<K, V>> splitNodeListLeft = getSplitNodeListLeft(cur);
            NodeGroup<K, V> nodeGroupLeft = createSplitNodeGroup(cur, splitNodeListLeft);
            Node<K, V> indexNode = createIndexNode(nodeGroupLeft);
            if (cur.parent == null) {
                // 创建新的根节点
                solveCreateNewRoot(indexNode, cur);
                return;
            }
            // 向上传递
            cur = solveInsertIndexNodeToParentGroup(cur.parent, indexNode);
        }
    }

    private NodeGroup<K, V> solveInsertIndexNodeToParentGroup(NodeGroup<K, V> parent, Node<K, V> indexNode) {
        int insertPosition = parent.searchInsertPosition(indexNode.key);
        parent.nodeList.add(insertPosition, indexNode);
        indexNode.next.parent = parent;
        return parent;
    }


    private void solveCreateNewRoot(Node<K, V> indexNode, NodeGroup<K, V> rightGroup) {
        NodeGroup<K, V> newRoot = new NodeGroup<>(order, false);
        newRoot.nodeList = new ArrayList<>();
        newRoot.nodeList.add(indexNode);
        indexNode.next.parent = newRoot;
        newRoot.next = rightGroup;
        rightGroup.parent = newRoot;
        this.root = newRoot;
    }

    private Node<K, V> createIndexNode(NodeGroup<K, V> nodeGroupLeft) {
        Node<K, V> node = new Node<>();
        node.key = nodeGroupLeft.lastNode().key;
        node.next = nodeGroupLeft;
        return node;
    }

    private NodeGroup<K, V> createSplitNodeGroup(NodeGroup<K, V> cur, List<Node<K, V>> nodeList) {
        NodeGroup<K, V> nodeGroupLeft = new NodeGroup<>(order, cur.bottom);
        NodeGroup<K, V> temp = cur.left;
        nodeGroupLeft.right = cur;
        // 维护双向链表
        cur.left = nodeGroupLeft;
        nodeGroupLeft.left = temp;
        if (temp != null) {
            temp.right = nodeGroupLeft;
        }
        nodeGroupLeft.nodeList = nodeList;
        if (cur == this.head) {
            this.head = nodeGroupLeft;
        }
        // 更新parent指针
        for (Node<K, V> node : nodeGroupLeft.nodeList) {
            if (node.next != null) {
                node.next.parent = nodeGroupLeft;
            }
        }
        return nodeGroupLeft;
    }

    public List<K> keySet() {
        List<K> res = new ArrayList<>();
        NodeGroup<K, V> cur = head;
        while (cur != null) {
            for (Node<K, V> node : cur.nodeList) {
                res.add(node.key);
            }
            cur = cur.right;
        }
        return res;
    }

    public void print() {
        List<NodeGroup<K, V>> queue = new ArrayList<>();
        queue.add(root);
        System.out.println(queue.get(0));
        while (!queue.isEmpty()) {
            List<NodeGroup<K, V>> temp = new ArrayList<>();
            for (NodeGroup<K, V> nodeGroup : queue) {
                if (nodeGroup.nodeList != null) {
                    for (Node<K, V> node : nodeGroup.nodeList) {
                        if (node.next != null) {
                            System.out.print(node.next);
                            temp.add(node.next);
                        }
                    }
                }
                if (nodeGroup.next != null) {
                    System.out.print(nodeGroup.next);
                    temp.add(nodeGroup.next);
                }
            }
            System.out.print("\n");
            queue = temp;
        }
    }


    private static class NodeGroup<K extends Comparable<K>, V> {
        List<Node<K, V>> nodeList;
        NodeGroup<K, V> parent;
        NodeGroup<K, V> next;
        NodeGroup<K, V> left;
        NodeGroup<K, V> right;
        int order;
        boolean bottom;

        @Override
        public String toString() {
            return nodeList.toString();
        }

        public NodeGroup(int order, boolean bottom) {
            this.order = order;
            this.nodeList = new ArrayList<>();
            this.bottom = bottom;
        }

        public Node<K, V> lastNode() {
            // 正常来讲不可能出现这种情况
            if (nodeList == null || nodeList.isEmpty()) {
                System.out.println("error");
                return new Node<>();
            }
            return nodeList.get(nodeList.size() - 1);
        }


        private int searchInsertPosition(K key) {
            // 二分搜索，搜索最左侧出现的位置
            int left = 0;
            int right = nodeList.size();
            while (left < right) {
                int mid = (left + right) / 2;
                if (nodeList.get(mid).key.compareTo(key) >= 0) {
                    right = mid;
                    continue;
                }
                left = mid + 1;
            }
            return right;
        }
    }

    private static class Node<K extends Comparable<K>, V> {
        K key;
        NodeGroup<K, V> next;

        @Override
        public String toString() {
            return key.toString();
        }
    }

    private static class Leaf<K extends Comparable<K>, V> extends Node<K, V> {
        V value;

        public Leaf(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

}