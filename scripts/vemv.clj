(ns vemv
  (:require
   [clojure.test]
   [formatting-stack.linters.one-resource-per-ns]
   [lambdaisland.deep-diff]
   [clojure.string :as string]
   [nedap.speced.def :as speced]))

(defn is=
  "Performs `(is = ...)`, reporting the diff between `expected` and `actual` when the test assertion fails."
  ([expected actual]
   (is= expected actual nil))

  ([expected actual desc]
   (clojure.test/testing (str (when (seq clojure.test/*testing-contexts*)
                                "\n") ;; place a newline so that prior strings and e.g. a large hashmap aren't rendered in the same line
                              (with-out-str
                                (-> expected
                                    (lambdaisland.deep-diff/diff actual)
                                    (lambdaisland.deep-diff/pretty-print (lambdaisland.deep-diff/printer {:print-color false})))))
     (clojure.test/is (= expected actual)
                      desc))))


(speced/defn ns-sym->filename* [^simple-symbol? ns-sym]
  (some-> (list 'ns ns-sym)
          (formatting-stack.linters.one-resource-per-ns/ns-decl->resource-path ".clj")
          formatting-stack.linters.one-resource-per-ns/resource-path->filenames
          first
          (string/replace "file:" "")))

(def ns-sym->filename (memoize ns-sym->filename*))
