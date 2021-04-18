from kazoo.client import KazooClient
from kazoo.client import KazooState
import threading
import logging
import os
import subprocess
import signal


last_tree = None
app_process = None


class Node:
    def __init__(self, name):
        self.name = name
        self.parent = None
        self.children = []

    def __eq__(self, other):
        return self.name == other.name


def get_tree_rec(zookeeper, node, path):
    children = zookeeper.get_children(path)
    if children is not None:
        for child in zookeeper.get_children(path):
            new_node = Node(child)
            node.children.append(new_node)
            get_tree_rec(zookeeper, new_node, path + "/" + child)


def get_tree(zookeeper):
    root = None
    if "z" in zookeeper.get_children("/"):
        root = Node("z")

        get_tree_rec(zookeeper, root, "/z")

    return root


def is_created_rec(new_node, old_node):
    for child in new_node.children:
        if child not in old_node.children:
            return True, "/" + child.name

    for child in old_node.children:
        if child not in new_node.children:
            return False, False

    for i in range(len(new_node.children)):
        before, after = is_created_rec(new_node.children[i], old_node.children[i])
        if before:
            return True, "/" + new_node.children[i].name + after
        elif after == "":
            continue
        else:
            return False, False

    return False, ""


def is_created(new_node, old_node):
    if new_node is None:
        return False, False

    if old_node is None:
        return True, "/" + new_node.name

    before, after = is_created_rec(new_node, old_node)
    if type(after) == str and len(after) > 0:
        return before, "/z" + after
    else:
        return before, after


def print_tree(node, indent=0):
    if node is not None:
        print("|  "*indent + node.name)
        for child in node.children:
            print_tree(child, indent+1)


def count_descendants(node):
    descendants = len(node.children)
    for child in node.children:
        descendants += count_descendants(child)

    return descendants


def read(zookeeper):
    while True:
        input()
        root = get_tree(zookeeper)
        print_tree(root)


def set_watcher(zookeeper, watch_path):
    @zookeeper.ChildrenWatch(watch_path)
    def watch_children(_):
        global last_tree
        root = get_tree(zookeeper)
        created, path = is_created(root, last_tree)
        last_tree = root
        if created:
            number_of_descendants = count_descendants(root)
            print(f"Number of descendants: {number_of_descendants}")
            set_watcher(zookeeper, path)


def open_app():
    global app_process
    app_process = subprocess.Popen(["spotify"], shell=False)


def close_app():
    global app_process
    if app_process is not None:
        pid = app_process.pid
        os.kill(pid, signal.SIGINT)


def main():
    global last_tree

    logging.basicConfig()

    zookeeper = KazooClient(hosts='127.0.0.1:2182,127.0.0.1:2183,127.0.0.1:2184')
    zookeeper.start()

    reading_thread = threading.Thread(target=read, args=(zookeeper,))
    reading_thread.start()

    last_tree = get_tree(zookeeper)

    @zookeeper.ChildrenWatch("/")
    def watch_children(children):
        global last_tree
        if last_tree is None and "z" in children:
            open_app()
            last_tree = get_tree(zookeeper)
            set_watcher(zookeeper, "/z")
        elif last_tree is not None and "z" not in children:
            close_app()
            last_tree = get_tree(zookeeper)

    while True:
        pass


main()
