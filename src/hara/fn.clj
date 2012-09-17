(ns hara.fn)

(defn manipulate*
  ([f x] (manipulate* f x {}))
  ([f x cs]
     (let [m-fn #(manipulate* f % cs)
           c (first (filter #(instance? (first %) x) cs))]
       (cond (not (nil? c))
             (let [c-ctor (second c)]
               (c-ctor (manipulate* f (f x) cs)))

             :else
             (cond
               (instance? clojure.lang.Atom x)   (atom (m-fn @x))
               (instance? clojure.lang.Ref x)    (ref (m-fn @x))
               (instance? clojure.lang.Agent x)  (agent (m-fn @x))
               (list? x)                         (apply list (map m-fn x))
               (vector? x)                       (vec (map m-fn x))

               (instance? clojure.lang.ISeq x)
               (map m-fn x)

               (instance? clojure.lang.IPersistentSet x)
               (apply hash-set (map m-fn x))

               (instance? clojure.lang.IPersistentMap x)
               (reduce #(apply assoc %1 %2) {} (map m-fn x))

               :else (f x))))))

(defn deref+ [x]
  (cond
   (instance? clojure.lang.IDeref x) @x
   :else x))

(defn deref*
  ([x] (deref* identity x))
  ([f x] (deref* f x {}))
  ([f x cs]
     (manipulate* (comp f deref+)
                  x
                  (into cs {clojure.lang.IDeref identity}))))
