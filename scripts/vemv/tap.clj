(ns vemv.tap)

(defn queue
  [& args]
  (apply list args))

(defn bounded-conj
  [^long n coll x]
  (let [b (== (count coll) n)]
    (cond-> (cons x coll)
      b rest)))

(def queue-size (atom 16))

(def taps-queue (atom (queue)))

(defn set-size! [n] (reset! queue-size n))

(defn reset-queue! [] (reset! taps-queue (queue)))

(defn qtap! [x] (swap! taps-queue #(bounded-conj @queue-size % x)))

(defn register! [] (add-tap qtap!))

(defn deregister! [] (remove-tap qtap!))

(defn view! [] taps-queue)

(register!)
