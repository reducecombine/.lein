(ns vemv.resources
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))

(defn ze-name ^String [^java.util.zip.ZipEntry ze]
  (.getName ze))

(defn split-path [path]
  (loop [acc () ^java.io.File path (io/file path)]
    (if path
      (recur (cons (.getName path) acc)
             (.getParentFile path))
      (vec acc))))

(defn render-path [p]
  (str/join "/" p))

(defn dir-entries
  ([dir] (dir-entries dir (constantly true)))
  ([dir want-file?]
   (forcat [e (.listFiles (io/file dir))]
           (cond (.isDirectory ^java.io.File e) (dir-entries e want-file?)
                 (want-file? e)   [e]))))

(defn zip-entries
  ([zip] (zip-entries zip (constantly true)))
  ([zip want-entry?]
   (with-open [zf (java.util.zip.ZipFile. (io/file zip))]
     (into [] (remove #_(do (println (ze-name %) "=>" (.endsWith (ze-name %) "/")))
                      #(.endsWith (ze-name %) "/")
                      (enumeration-seq
                       (.entries zf)))))))

(defn relativize [base path]
  (let [base (when base (.getCanonicalFile (io/file base)))
        path (io/file path)]
    (loop [acc ()
           ^java.io.File path' path]
      (when-not path'
        (throw (ex-info (str "Resource dir not contained in project root"
                             {:base base :path path})
                        {:base base :path path})))
      (if (= base path')
        (vec acc)
        (recur (cons (.getName path') acc)
               (.getParentFile path'))))))

(defn system-classpath-roots []
  (str/split (System/getProperty "java.class.path") #":"))

(defn classpath-resources
  ([]
   (classpath-resources (system-classpath-roots)))

  ([roots] (if (string? roots)
             (recur (str/split roots #":"))
             (forcat [r roots
                      :let [f (.getCanonicalFile (io/file r))
                            ze-meta {:classpath-entry f}]]
                     (cond
                       (.isDirectory f) (for [de (dir-entries f)]
                                          (with-meta (relativize f de)
                                            ze-meta))
                       (.isFile f)      (for [ze (zip-entries f)]
                                          (with-meta (split-path (str ze))
                                            ze-meta)))))))

(defn native-deps []
  (->> (classpath-resources)
       (filter (comp (fn [x]
                       (or (-> x str (str/ends-with? ".so"))
                           (-> x str (str/ends-with? ".dynlib"))))
                     last))
       (clojure.pprint/pprint )))
