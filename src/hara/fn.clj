(ns hara.fn
  (:refer-clojure :exclude [send]))


(defn call-if-not-nil
  ([f] (if-not (nil? f) (f)) )
  ([f v] (if-not (nil? f) (f v)))
  ([f v1 v2] (if-not (nil? f) (f v1 v2)))
  ([f v1 v2 v3] (if-not (nil? f) (f v1 v2 v3)))
  ([f v1 v2 v3 v4] (if-not (nil? f) (f v1 v2 v3 v4)))
  ([f v1 v2 v3 v4 v5] (if-not (nil? f) (f v1 v2 v3 v4 v5)))
  ([f v1 v2 v3 v4 v5 v6] (if-not (nil? f) (f v1 v2 v3 v4 v5 v6)))
  ([f v1 v2 v3 v4 v5 v6 & vs] (if-not (nil? f) (apply f v1 v2 v3 v4 v5 v6 vs))))

(defn look-up [coll ks]
  (reduce (fn [coll k] (call-if-not-nil coll k))
          coll
          ks))

(defn send
  ([obj kw] (call-if-not-nil (obj kw) obj))
  ([obj kw v] (call-if-not-nil (obj kw) obj v))
  ([obj kw v1 v2] (call-if-not-nil (obj kw) obj v1 v2))
  ([obj kw v1 v2 v3] (call-if-not-nil (obj kw) obj v1 v2 v3))
  ([obj kw v1 v2 v3 v4] (call-if-not-nil (obj kw) obj v1 v2 v3 v4))
  ([obj kw v1 v2 v3 v4 & vs] (apply call-if-not-nil (obj kw) obj v1 v2 v3 v4 vs)))

;; watch

(defn watch-for-change [kv f]
  (fn [k rf p n & xs] ;; xs := [t func args]
    (let [pv (look-up p kv)
          nv (look-up n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (apply f k rf pv nv xs)))))

(defn watch-elem-for-change [kv f]
  (fn [k ov rf p n]
    (let [pv (look-up p kv)
          nv (look-up n kv)]
      (cond (and (nil? pv) (nil? nv)) nil
            (= pv nv) nil
            :else (f k ov rf pv nv)))))


;; higher order functions

(defn manipulate*
  ([f x] (manipulate* f x {}))
  ([f x cs]
     (let [m-fn    #(manipulate* f % cs)
           pred-fn (fn [pd]
                     (cond (instance? Class pd) #(instance? pd %)
                           (fn? pd) pd
                           :else (constantly false)))
           custom? #((pred-fn (:pred %)) x)
           c (first (filter custom? cs))]
       (cond (not (nil? c))
             (let [ctor (or (:ctor c) identity)
                   dtor (or (:dtor c) identity)]
               (ctor (manipulate* f (dtor x) cs)))

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

(defn- deref+ [x]
  (cond
   (instance? clojure.lang.IDeref x) @x
   :else x))

(defn deref*
  ([x] (deref* identity x))
  ([f x] (deref* f x []))
  ([f x cs]
     (manipulate* f
                  x
                  (conj cs {:pred clojure.lang.IDeref
                            :ctor identity
                            :dtor deref}))))

;; multi-threaded
(defmacro exec-seq [threaded bindings & body]
  `(cond (= ~threaded :single)
         (doseq ~bindings ~@body)
         (= ~threaded :multi)
         (doseq ~bindings (future ~@body))))