(ns compliment.sources.local-bindings
  "https://github.com/alexander-yakushev/compliment/issues/76"
  (:require [compliment.sources :refer [defsource]]
            [compliment.sources.ns-mappings :refer [var-symbol? dash-matches?]]))

(def let-like-forms '#{let if-let when-let if-some when-some loop with-open
                       dotimes with-local-vars})

(def defn-like-forms '#{defn defn- fn defmacro})

(def doseq-like-forms '#{doseq for})

(def letfn-like-forms '#{letfn})

(defn parse-binding
  "Given a binding node returns the list of local bindings introduced by that
  node. Handles vector and map destructuring."
  [binding-node]
  (cond (vector? binding-node)
        (mapcat parse-binding binding-node)

        (map? binding-node)
        (let [normal-binds (->> (keys binding-node)
                                (remove keyword?)
                                (mapcat parse-binding))
              keys-binds (mapcat binding-node [:keys :strs :syms])
              as-binds (if-let [as (:as binding-node)]
                         [as] ())]
          (concat normal-binds keys-binds as-binds))

        (not (#{'& '_} binding-node))
        [binding-node]))

(defn parse-fn-body
  "Extract function name and arglists from the function body, return list of all
  completable variables."
  [fn-body]
  (let [fn-name (when (symbol? (first fn-body))
                  (first fn-body))
        fn-body (if fn-name (rest fn-body) fn-body)]
    (cond->
        (mapcat parse-binding
                (loop [[c & r] fn-body, bnodes []]
                  (cond (nil? c) bnodes
                        (list? c) (recur r (conj bnodes (first c))) ;; multi-arity case
                        (vector? c) c                               ;; single-arity case
                        :else (recur r bnodes))))
      fn-name (conj fn-name))))

(defn extract-local-bindings
  "When given a form that has a binding vector traverses that binding vector and
  returns the list of all local bindings."
  [form]
  (when (list? form)
    ;; vemv patch
    (let [sym (-> form first name symbol)]
      (cond (let-like-forms sym)
            (mapcat parse-binding (take-nth 2 (second form)))

            (defn-like-forms sym) (parse-fn-body (rest form))

            (letfn-like-forms sym)
            (mapcat parse-fn-body (second form))

            (doseq-like-forms sym)
            (->> (partition 2 (second form))
                 (mapcat (fn [[left right]]
                           (if (= left :let)
                             (take-nth 2 right) [left])))
                 (mapcat parse-binding))

            (= 'as-> sym)         [(nth form 2)]))))

(defn extract-local-binding-mappings
  "When given a form that has a binding vector traverses that binding vector and
  returns the list of all local bindings."
  [form]
  (when (list? form)
    (let [sym (-> form first name symbol)]
      (cond
        (let-like-forms sym)
        (->> form second (partition 2) (mapv vec) (into {}))

        ;; could be done, not gonna bother for now
        (defn-like-forms sym) {}

        ;;same
        (letfn-like-forms sym)
        {}

        (doseq-like-forms sym)
        (->> form
             second
             (partition 2)
             (map (fn [[left right :as x]]
                    (vec (if (= left :let)
                           right
                           x))))
             (into {}))

        (= 'as-> sym)         {}))))

(comment
  (extract-local-binding-mappings '(let [a [1 2 3]])))

(defn- distinct-preserve-tags
  "Like `distinct` but keeps symbols that have type tag with a higher priority."
  [coll]
  (->> coll
       (sort (fn [x y]
               (let [tx (:tag (meta x))
                     ty (:tag (meta y))]
                 (cond (and tx (not ty)) -1
                       (and (not tx) ty) 1
                       :else             0))))
       distinct))

(defn bindings-from-context
  "Returns all local bindings that are established inside the given context."
  [ctx]
  (try (->> (mapcat (comp extract-local-bindings :form) ctx)
            (filter symbol?)
            distinct-preserve-tags)
       (catch Exception ex ())))

(defn binding-mappings-from-context
  "Returns all local bindings that are established inside the given context."
  [ctx]
  (try (mapcat (comp extract-local-binding-mappings :form) ctx)
       (catch Exception ex ())))

(defn candidates
  "Returns a list of local bindings inside the context that match prefix."
  [prefix _ context]
  (when (var-symbol? prefix)
    (for [binding (bindings-from-context context)
          :let [binding (name binding)]
          :when (dash-matches? prefix binding)]
      {:candidate binding, :type :local})))

(defsource ::local-bindings
  :candidates #'candidates
  :doc (constantly nil))
