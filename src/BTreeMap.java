import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BTreeMap<K extends Comparable<K>, V> {

    private final int order;

    private final int minNumber;

    private final int maxNumber;

    private NodeGroup<K, V> root = null;

    public BTreeMap(int order) {
        this.order = order;
        this.minNumber = this.order / 2;
        this.maxNumber = this.order;
    }

    public V put(K key, V value) {
        return putVal(key, value);
    }

    public V remove(K key) {
        return removeKey(key);
    }

    private V removeKey(K key) {
        Node<K, V> node = new Node<>(key, null);
        FindResult<K, V> findResult = find(node);
        if (findResult.findSame) {
            V findResultValue = findResult.nodeGroup.nodeList.get(findResult.position).value;
            doDelete(findResult.nodeGroup, findResult.position);
            return findResultValue;
        }
        return null;
    }

    private void doDelete(NodeGroup<K, V> group, int position) {

        // to transform to delete leaf node
        Node<K, V> waitDeleteNode = group.nodeList.get(position);
        int waitDeletePosition = position;

        // if this node is leaf node we're done
        if (waitDeleteNode.minorNodeGroup != null) {

            // to find a leaf node to replace it
            // just find the first node smaller than it
            NodeGroup<K, V> cur = waitDeleteNode.minorNodeGroup;
            while (cur.nextNode != null) {
                cur = cur.nextNode;
            }
            Node<K, V> last = cur.nodeList.getLast();
            waitDeleteNode.key = last.key;
            waitDeleteNode.value = last.value;

            group = cur;
            waitDeleteNode = last;
            waitDeletePosition = cur.nodeList.size() - 1;

        }

        for (; ; ) {

            group.nodeList.remove(waitDeletePosition);

            // is group is valid or group is root node we're done
            if (valid(group) || group == root) {
                break;
            }

            int parentPosition = group.parentNodeGroup.search(waitDeleteNode);

            NodeGroup<K, V> brotherNodeGroup = findBrotherNodeGroup(group, parentPosition);

            boolean brotherLeft = group == group.parentNodeGroup.nextNode;

            if (full(brotherNodeGroup)) {

                // get node from brother node then we're done
                // it just equals tree rotate
                if (brotherLeft) {
                    // just get kid from left node group
                    Node<K, V> waitMove = brotherNodeGroup.nodeList.removeLast();

                    group.nodeList.addFirst(waitMove);

                    exchangeNodeValue(waitMove, group.parentNodeGroup.nodeList.get(parentPosition - 1));

                    NodeGroup<K, V> temp = waitMove.minorNodeGroup;

                    waitMove.minorNodeGroup = brotherNodeGroup.nextNode;
                    if (waitMove.minorNodeGroup != null) {
                        waitMove.minorNodeGroup.parentNodeGroup = group;
                    }

                    brotherNodeGroup.updateNextNode(temp);

                } else {

                    Node<K, V> waitMove = brotherNodeGroup.nodeList.removeFirst();
                    group.nodeList.addLast(waitMove);

                    exchangeNodeValue(waitMove, group.parentNodeGroup.nodeList.get(parentPosition));

                    NodeGroup<K, V> temp = waitMove.minorNodeGroup;

                    waitMove.minorNodeGroup = group.nextNode;

                    group.updateNextNode(temp);

                }

                break;
            }

            // do merge
            int midNodePosition;

            if (brotherLeft) {
                midNodePosition = parentPosition - 1;
            } else {
                midNodePosition = parentPosition;
            }

            Node<K, V> midNode = group.parentNodeGroup.nodeList.get(midNodePosition);
            NodeGroup<K, V> nodeGroupLeft;
            NodeGroup<K, V> nodeGroupRight;

            /*
                1. justify left and right node group
                2. put middle node in left node
                3. put all node in left node
                4. update pointer
                    make middle node's minorNode is left node group
                    middle node's next node's minor node is left node
                    left node's max node is right node's max node
             */

            if (brotherLeft) {
                nodeGroupLeft = brotherNodeGroup;
                nodeGroupRight = group;
            } else {
                nodeGroupLeft = group;
                nodeGroupRight = brotherNodeGroup;
            }

            nodeGroupLeft.nodeList.addLast(midNode);
            midNode.minorNodeGroup = nodeGroupLeft.nextNode;
            nodeGroupLeft.updateNextNode(nodeGroupRight.nextNode);

            for (int i = 0; i < nodeGroupRight.nodeList.size(); i++) {
                Node<K, V> node = nodeGroupRight.nodeList.get(i);
                nodeGroupLeft.nodeList.addLast(node);
                if (node.minorNodeGroup != null) {
                    node.minorNodeGroup.parentNodeGroup = nodeGroupLeft;
                }
            }

            if (midNodePosition == group.parentNodeGroup.nodeList.size() - 1) {
                group.parentNodeGroup.updateNextNode(nodeGroupLeft);
            } else {
                group.parentNodeGroup.nodeList.get(midNodePosition + 1).minorNodeGroup = nodeGroupLeft;
            }
            group = group.parentNodeGroup;
            waitDeleteNode = midNode;
            waitDeletePosition = midNodePosition;
        }

        if (group == root && group.nodeList.isEmpty()) {
            root = group.nextNode;
        }

    }

    private void exchangeNodeValue(Node<K, V> a, Node<K, V> b) {
        K tempK = a.key;
        a.key = b.key;
        b.key = tempK;
        V tempV = a.value;
        a.value = b.value;
        b.value = tempV;
    }

    private static <K extends Comparable<K>, V> NodeGroup<K, V> findBrotherNodeGroup(NodeGroup<K, V> group, int parentPosition) {
        NodeGroup<K, V> brotherNodeGroup;
        if (group == group.parentNodeGroup.nextNode) {
            // find left node group
            brotherNodeGroup = group.parentNodeGroup.nodeList.getLast().minorNodeGroup;
        } else {
            // find right node group
            if (parentPosition == group.parentNodeGroup.nodeList.size() - 1) {
                brotherNodeGroup = group.parentNodeGroup.nextNode;
            } else {
                brotherNodeGroup = group.parentNodeGroup.nodeList.get(parentPosition + 1).minorNodeGroup;
            }
        }
        return brotherNodeGroup;
    }


    private boolean full(NodeGroup<K, V> group) {
        return group.nodeList.size() > minNumber;
    }

    private boolean valid(NodeGroup<K, V> group) {
        return group.nodeList.size() >= minNumber;
    }

    private void initRoot() {
        if (this.root == null) {
            this.root = new NodeGroup<>(this.order);
        }
    }

    private V putVal(K key, V value) {

        // ensure this tree got a root
        initRoot();

        // create new node
        Node<K, V> node = new Node<>(key, value);

        // find should insert position
        FindResult<K, V> findResult = find(node);

        // find same key !!
        if (findResult.findSame) {
            // just update value and return old value
            Node<K, V> oldNode = findResult.nodeGroup.nodeList.get(findResult.position);
            V temp = oldNode.value;
            oldNode.value = node.value;
            return temp;
        }

        insertAndBalance(findResult.nodeGroup, node, findResult.position);


        return null;
    }

    private void createNewRoot() {
        this.root = new NodeGroup<>(this.order);
    }

    private void printTree() {
        List<NodeGroup<K, V>> queue = new ArrayList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            List<NodeGroup<K, V>> temp = new ArrayList<>();
            List<List<K>> out = new ArrayList<>();
            for (NodeGroup<K, V> kvNodeGroup : queue) {
                List<K> tempK = new ArrayList<>();
                for (Node<K, V> kvNode : kvNodeGroup.nodeList) {
                    tempK.add(kvNode.key);
                    if (kvNode.minorNodeGroup != null) {
                        temp.add(kvNode.minorNodeGroup);
                    }
                }
                out.add(tempK);
                if (kvNodeGroup.nextNode != null) {
                    temp.add(kvNodeGroup.nextNode);
                }
            }
            System.out.println(out);
            queue = temp;
        }
    }

    public List<K> list() {
        List<K> result = new ArrayList<>();
        orderTree(result, root);
        return result;
    }

    private void orderTree(List<K> result, NodeGroup<K, V> nodeGroup) {
        if (nodeGroup == null) {
            return;
        }
        for (Node<K, V> node : nodeGroup.nodeList) {
            orderTree(result, node.minorNodeGroup);
            result.add(node.key);
        }
        orderTree(result, nodeGroup.nextNode);
    }

    private void insertAndBalance(NodeGroup<K, V> nodeGroup, Node<K, V> node, int position) {
        for (; ; ) {

            if (nodeGroup.nodeList.size() + 1 <= maxNumber) {
                nodeGroup.nodeList.add(position, node);
                if (node.minorNodeGroup != null) {
                    node.minorNodeGroup.parentNodeGroup = nodeGroup;
                }
                return;
            }

            NodeGroup<K, V> nodeGroupSplit = new NodeGroup<>(order);

            nodeGroup.nodeList.add(position, node);
            if (node.minorNodeGroup != null) {
                node.minorNodeGroup.parentNodeGroup = nodeGroup;
            }

            // do node split
            int midPosition = (maxNumber + 1) / 2;

            node = nodeGroup.nodeList.get(midPosition);

            // update old node and split node right pointer
            nodeGroupSplit.updateNextNode(nodeGroup.nextNode);
            nodeGroup.updateNextNode(node.minorNodeGroup);


            for (int i = midPosition + 1; i < nodeGroup.nodeList.size(); i++) {
                Node<K, V> waitSplitNode = nodeGroup.nodeList.get(i);
                nodeGroupSplit.nodeList.add(waitSplitNode);
                if (waitSplitNode.minorNodeGroup != null) {
                    waitSplitNode.minorNodeGroup.parentNodeGroup = nodeGroupSplit;
                }
            }

            nodeGroup.nodeList.subList(midPosition, nodeGroup.nodeList.size()).clear();

            // update mid node minor node group as left node group
            node.minorNodeGroup = nodeGroup;

            if (nodeGroup.parentNodeGroup == null) {
                createNewRoot();

                root.nextNode = nodeGroupSplit;

                nodeGroupSplit.parentNodeGroup = root;
                nodeGroup.parentNodeGroup = root;

                // do next round
                nodeGroup = root;
                position = 0;

                continue;
            }

            NodeGroup<K, V> parentNodeGroup = nodeGroup.parentNodeGroup;
            nodeGroupSplit.parentNodeGroup = parentNodeGroup;

            position = parentNodeGroup.search(node);

            if (nodeGroup == parentNodeGroup.nextNode) {
                parentNodeGroup.nextNode = nodeGroupSplit;
            } else {
                parentNodeGroup.nodeList.get(position).minorNodeGroup = nodeGroupSplit;
            }

            // do next round
            nodeGroup = parentNodeGroup;

        }
    }

    public static void main(String[] args) {
        BTreeMap<Integer, Integer> map = new BTreeMap<>(3);
        List<Integer> data = new ArrayList<>();
        int testNumber = 1000000;
        for (int i = 0; i < testNumber; i++) {
            data.add(i);
        }
        long startTime = System.currentTimeMillis();
        Collections.shuffle(data);
        for (Integer item : data) {
            map.put(item, item);
        }
        System.out.println(System.currentTimeMillis() - startTime);

        Collections.shuffle(data);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < testNumber - 100; i++) {
            map.remove(data.get(i));
        }
        System.out.println(System.currentTimeMillis() - startTime);
        System.out.println(map.list());
        List<Integer> list = data.subList(testNumber - 100, testNumber);
        Collections.sort(list);
        System.out.println(list);

    }

    private FindResult<K, V> find(Node<K, V> node) {
        NodeGroup<K, V> current = root;
        for (; ; ) {
            int position = current.search(node);

            // if this result node group is empty just return
            if (current.nodeList.isEmpty()) {
                return new FindResult<>(current, position, false);
            }

            // if we find same key node stop search and return this node
            if (position < current.nodeList.size() && judgeKeySame(current.nodeList.get(position), node)) {
                return new FindResult<>(current, position, true);
            }

            NodeGroup<K, V> nextNodeGroup = null;

            if (position == current.nodeList.size()) {
                nextNodeGroup = current.nextNode;
            } else {
                nextNodeGroup = current.nodeList.get(position).minorNodeGroup;
            }

            if (nextNodeGroup == null) {
                return new FindResult<>(current, position, false);
            }

            // update current Node and find again
            current = nextNodeGroup;
        }
    }

    private boolean judgeKeySame(Node<K, V> a, Node<K, V> b) {
        return a.key.compareTo(b.key) == 0;
    }

    private static class FindResult<K extends Comparable<K>, V> {
        NodeGroup<K, V> nodeGroup;
        int position;
        boolean findSame;

        public FindResult(NodeGroup<K, V> nodeGroup, int position, boolean findSame) {
            this.nodeGroup = nodeGroup;
            this.position = position;
            this.findSame = findSame;
        }
    }

    private static class NodeGroup<K extends Comparable<K>, V> {

        private final List<Node<K, V>> nodeList;
        private NodeGroup<K, V> nextNode;
        NodeGroup<K, V> parentNodeGroup;

        public NodeGroup(int order) {
            nodeList = new ArrayList<>(order);
        }

        void updateNextNode(NodeGroup<K, V> nodeGroup) {
            this.nextNode = nodeGroup;
            if (nodeGroup != null) {
                nodeGroup.parentNodeGroup = this;
            }
        }

        // binary search to find left node in node group
        private int search(Node<K, V> node) {
            int left = 0;
            int right = nodeList.size();
            while (left < right) {
                int mid = (left + right) / 2;
                K midKey = nodeList.get(mid).key;
                if (midKey.compareTo(node.key) == 0) {
                    return mid;
                }
                if (midKey.compareTo(node.key) > 0) {
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
        V value;
        NodeGroup<K, V> minorNodeGroup;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key.toString();
        }
    }

}
