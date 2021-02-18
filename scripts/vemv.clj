(ns vemv
  (:require
   [clojure.test]
   [lambdaisland.deep-diff]))

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
