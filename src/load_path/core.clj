(ns load-path.core
  (:require [clojure.java.io      :refer [file as-url resource]]
            [clojure.string       :as    clj-str]
            [cemerick.pomegranate :refer [get-classpath]])
  (:import [java.io  FileNotFoundException]
           [java.net URL MalformedURLException URLClassLoader]))

(defn- add-trailing-slash [path]
  (if (.endsWith path "/")
    path
    (str path "/")))

(defn- jar-entries [jar-url]
  (.entries (.getJarFile (.openConnection jar-url))))

(def ^:private files-in-jar
  (memoize
    (fn [jar-path]
      (loop [entries (jar-entries (as-url jar-path)) results []]
        (if (.hasMoreElements entries)
          (recur entries (conj results (.getName (.nextElement entries))))
          results)))))

(defn- file-in-jar-with-base-path? [jar-path base-path]
  (some
    #(.startsWith % base-path)
    (files-in-jar jar-path)))

(defn- search-jar [base-url path]
  (let [jar-url (str (URL. "jar" "" (str base-url "!/")))]
    (if (file-in-jar-with-base-path? jar-url path)
      (add-trailing-slash (str jar-url path)))))

(defn- search-fs [base-path path]
  (let [f (file (str base-path "/" path))]
    (when (and (.exists f) (.isDirectory f))
      (add-trailing-slash (str (.toURI f))))))

(defn- search-in-path [base-url path]
  (let [base-path (.getPath base-url)]
    (if (.endsWith base-path ".jar")
      (search-jar base-url path)
      (search-fs base-path path))))

(defn- search-class-path [path]
  (->> (get-classpath)
    (map as-url)
    (map #(search-in-path % path))))

(defn- search-working-directory [path]
  (let [f (file path)]
    (when (and (.exists f) (.isDirectory f))
      (str (.toURI f)))))

(defn- absolute-path-with-protocol? [path]
  (.startsWith path "jar:"))

(defn- normalize-path [path]
  (if (.startsWith path "file:")
    (clj-str/replace-first path "file:" "")
    path))

(defn- absolute-paths-with-protocol [path]
  (let [normalized (normalize-path path)]
    (if (absolute-path-with-protocol? normalized)
      [normalized]
      (keep identity
            (cons (search-working-directory normalized)
                  (search-class-path normalized))))))

(defn read-stream [stream]
  (let [sb (StringBuilder.)]
    (with-open [stream stream]
      (loop [c (.read stream)]
        (if (neg? c)
          (str sb)
          (do
            (.append sb (char c))
            (recur (.read stream))))))))

(defn- working-dir-class-loader []
  (let [working-dir-url (.toURL (.toURI (-> (System/getProperty "user.dir") file)))
        context-class-loader (.getContextClassLoader (Thread/currentThread))]
    (URLClassLoader. (into-array URL [working-dir-url]) context-class-loader)))

(defn safe-read-file
  ([file-name] (safe-read-file file-name (working-dir-class-loader)))
  ([file-name class-loader]
   (when-let [stream (.getResourceAsStream class-loader file-name)]
     (read-stream stream))))

(defn read-file
  ([file-name] (read-file file-name (working-dir-class-loader)))
  ([file-name class-loader]
    (let [file-body (safe-read-file file-name class-loader )]
      file-body
      (throw (FileNotFoundException. file-name)))))

(def class-loader-for-load-paths
  (memoize
    (fn [load-paths]
      (let [absolute-paths (mapcat absolute-paths-with-protocol (set load-paths))
            urls (set (map as-url absolute-paths))]
        (URLClassLoader. (into-array URL urls))))))

(defn- absolute-and-relative-paths [base-path absolute-path]
  {:absolute-path absolute-path
   :relative-path (clj-str/replace-first absolute-path base-path "")})

(defn- jar-directory? [url]
  (= "jar" (.getProtocol url)))

(defmulti list-files-on-url #(if (jar-directory? %) :jar :file-system))

(defn- files-in-jar-with-base-path [jar-path base-path]
  (filter
    #(.startsWith % base-path)
    (files-in-jar jar-path)))

(defn- split-jar-and-path [jar-path]
  (let [end-of-jar (+ 2 (.indexOf jar-path "!/"))]
    [(.substring jar-path 0 end-of-jar)
     (.substring jar-path end-of-jar (.length jar-path))]))

(defmethod list-files-on-url :jar [url]
  (let [[jar-path base-path] (split-jar-and-path (str url))]
    (map
      (partial absolute-and-relative-paths base-path)
      (files-in-jar-with-base-path jar-path base-path))))

(defmethod list-files-on-url :file-system [url]
  (let [base-path (.getPath url)]
    (map
      (partial absolute-and-relative-paths base-path)
      (keep
        (fn [f]
          (if (.isFile f)
            (.getAbsolutePath f)
            nil))
        (-> base-path file file-seq)))))

(defn list-files [load-paths]
  (let [class-loader (class-loader-for-load-paths load-paths)
        urls (.getURLs class-loader)]
    (mapcat list-files-on-url urls)))

(defn read-file-from-load-path [file-name load-paths extensions]
  (let [class-loader (class-loader-for-load-paths load-paths)]
    (some
      identity
      (map
        (fn [extension]
          (let [file-name-with-extension (str file-name "." extension)]
            (when-let [file-body (safe-read-file file-name-with-extension class-loader)]
              {:body          file-body
               :extension     extension
               :relative-path file-name-with-extension
               :absolute-path (normalize-path (str (resource file-name-with-extension class-loader)))})))
        extensions))))

