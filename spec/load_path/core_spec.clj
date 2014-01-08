(ns load-path.core-spec
  (:require [speclj.core     :refer :all]
            [clojure.java.io :refer [file resource]]
            [clojure.string  :as clj-str]
            [load-path.core  :refer :all]))

(describe "load-path.core"

  (it "reads a file when the load path is relative to the working directory"
    (let [content (read-file-from-load-path "test1" ["spec/resources1/dir"] ["txt"])]
      (should= "i'm a txt file\n" (:body content))
      (should= "test1.txt" (:relative-path content))
      (should (.contains (:absolute-path content) "test1.txt"))
      (should= "txt" (:extension content))))

  (it "reads a file when the load path is relative to the classpath"
    (let [content (read-file-from-load-path "test1" ["resources1/dir"] ["txt"])]
      (should= "i'm a txt file\n" (:body content))))

  (it "reads a file when the file is nested in the working directory"
    (let [content (read-file-from-load-path "dir/test1" ["spec/resources1"] ["txt"])]
      (should= "i'm a txt file\n" (:body content))))

  (it "reads a file when the file is nested on the class path"
    (let [content (read-file-from-load-path "dir/test1" ["resources1"] ["txt"])]
      (should= "i'm a txt file\n" (:body content))))

  (it "reads a file when the load path has more than one path"
    (let [content (read-file-from-load-path "dir/test1" ["spec/resources1" "spec/resources2"] ["txt"])]
      (should= "i'm a txt file\n" (:body content))))

  (it "reads from multiple folders that have the same name from two separate class paths"
    (let [test1 (read-file-from-load-path "test1" ["dir"] ["txt"])
          test2 (read-file-from-load-path "test2" ["dir"] ["txt"])]
      (should= "i'm a txt file\n" (:body test1))
      (should= "i'm a txt file2\n" (:body test2))))

  (it "reads from multiple folders that have the same name from the working dir and class path"
    (let [test1 (read-file-from-load-path "test1" ["spec/resources1/dir"] ["txt"])
          test2 (read-file-from-load-path "test2" ["spec/resources1/dir"] ["txt"])]
      (should= "i'm a txt file\n" (:body test1))
      (should= "i'm a txt file2\n" (:body test2))))

  (it "reads a png file"
    (let [png (read-file-from-load-path "joodo" ["spec/resources1"] ["png"])]
      (should= 6533 (count (:body png)))))

  (with resources2-files ["dir/test2.txt" "spec/resources1/dir/test2.txt"])

  (it "lists files on the load path from a working directory"
    (let [files (list-files ["spec/resources2"])
          first-file (first files)
          relative-paths (sort (map :relative-path files))]
      (should= @resources2-files relative-paths)
      (should (.contains (:absolute-path first-file) (:relative-path first-file)))
      (should-not= (:absolute-path first-file) (:relative-path first-file))))

  (it "lists files on the load path from a directory on the class path"
    (let [files (list-files ["resources2"])
          first-file (first files)
          relative-paths (sort (map :relative-path files))]
      (should= @resources2-files relative-paths)))

  (it "lists files with an absolute path to a folder"
    (let [folder (file "spec/resources2")
          files (list-files [(.getAbsolutePath folder)])
          first-file (first files)
          relative-paths (sort (map :relative-path files))]
      (should= @resources2-files relative-paths)))

  (it "lists files with an absolute path to a folder on the classpath"
    (let [resource-file (resource "resources2/dir/test2.txt")
          cp-root (first (clj-str/split (str resource-file) (re-pattern "/resources2/dir/test2.txt") 2))
          absoulte-resource-directory (str cp-root "/resources2/")
          files (list-files [absoulte-resource-directory])
          first-file (first files)
          relative-paths (sort (map :relative-path files))]
      (should= @resources2-files relative-paths)))

  (it "returns unique files"
    (let [files (list-files ["resources2" "resources2"])
          first-file (first files)
          relative-paths (sort (map :relative-path files))]
      (should= @resources2-files relative-paths)))

  )
