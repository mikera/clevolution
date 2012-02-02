(ns clevlib
  (:refer-clojure :exclude [* + - and or min max mod])
  (:use [com.nodename.evolution.image_ops.zeroary.gradient :only [X Y]]
        [com.nodename.evolution.image_ops.zeroary.noise :only [bw-noise]]
        [com.nodename.evolution.file-io :only [read-image-from-file]]
        [com.nodename.evolution.image_ops.unary :only [abs sin cos log inverse blur *]]
        [com.nodename.evolution.image_ops.binary :only [+ - and or xor min max mod]]
        [com.nodename.evolution.file-io :only [save-image]] :reload-all))

;; http://blog.jayway.com/2011/03/13/dbg-a-cool-little-clojure-macro/
(defmacro dbg [& body]
  `(let [x# ~@body]
     (println (str "dbg: " (quote ~@body) "=" x#))
     x#))


(def image-width 400)
(def image-height 400)

(defn- int-range
  [lo hi]
  (+ lo (rand-int (- hi lo))))

(defn- float-range
  [lo hi]
  (+ lo (rand (- hi lo))))

(defn- make-X
	[]
	(with-meta (list 'X image-width image-height) {:arity 0}))

(defn- make-Y
	[]
	(with-meta (list 'Y image-width image-height) {:arity 0}))

(defn make-bw-noise
  []
  (let [seed (int-range 50 1000)
        octaves (int-range 1 10)
        falloff (float-range 0.1 1.0)]
    (with-meta (list 'bw-noise seed octaves falloff image-width image-height) {:arity 0})))

;; TODO cache images
(defn- make-read [uri]
  (with-meta (list 'read-image-from-file uri) {:arity 0}))

(defn- make-abs
  []
  (with-meta (list 'abs) {:arity 1}))

(defn- make-sin
  []
  (with-meta (list 'sin) {:arity 1}))

(defn- make-cos
  []
  (with-meta (list 'cos) {:arity 1}))

(defn- make-log
  []
  (with-meta (list 'log) {:arity 1}))

(defn- make-inverse
  []
  (with-meta (list 'inverse) {:arity 1}))

(defn- make-*
  []
  (let [factor (float-range 0.5 2.0)]
    (with-meta (list '* factor) {:arity 1})))

(defn- make-blur
  []
  (let [radius (float-range 0.0 1.0)
        sigma (float-range 0.5 2.0)]
    (with-meta (list 'blur radius sigma) {:arity 1})))

(defn- make-+
  []
  (with-meta (list '+) {:arity 2}))

(defn- make--
  []
  (with-meta (list '-) {:arity 2}))

(defn- make-and
  []
  (with-meta (list 'and) {:arity 2}))

(defn- make-or
  []
  (with-meta (list 'or) {:arity 2}))

(defn- make-xor
  []
  (with-meta (list 'xor) {:arity 2}))

(defn- make-min
  []
  (with-meta (list 'min) {:arity 2}))

(defn- make-max
  []
  (with-meta (list 'max) {:arity 2}))

(defn- make-mod
  []
  (with-meta (list 'mod) {:arity 2}))



(defn- zeroary-ops-makers
  []
  (vector (make-X) (make-Y) (make-bw-noise)))

(defn- unary-ops-makers
  []
  (vector (make-abs) (make-sin) (make-cos) (make-log) (make-inverse) (make-*) (make-blur)))

(defn- binary-ops-makers
  []
  (vector (make-+) (make--) (make-and) (make-or) (make-xor) (make-min) (make-max) (make-mod)))

                
(defn- select-random-op
  "Select a random element from one or more vectors"
  [ops & more]
    (let [ops 
          (if more
            (vec (concat ops (reduce concat more)))
            ops)]
      (ops (rand-int (count ops)))))



(defn- append-without-flattening
  "Add list-to-append as a single last element of orig-list"
  [orig-list list-to-append]
      (seq (conj (vec orig-list) list-to-append)))

(defn- compose-ops
  ([binary-op op0 op1]
    (compose-ops (compose-ops binary-op op0) op1))
  ([unary-op op]
    (if (seq? unary-op)
      (append-without-flattening unary-op op)
      (conj unary-op op))))


(defn- foo
  [max-depth zeroary-ops]
    (let [op (if (== max-depth 0)
               (select-random-op zeroary-ops)
               (select-random-op zeroary-ops (unary-ops-makers) (binary-ops-makers)))
          arity ((meta op) :arity)]
      (loop [i 0
            expression op]
        (cond
          (== i arity) expression
          :else (recur (inc i) (compose-ops expression (foo (dec max-depth) zeroary-ops)))))))


;; TODO manage width and height of input images
;; TODO can we make this work with delaying evaluation of the zeroary-ops-makers (bw-noise specifically) until each time they are used?
;; because now the bw-noise parameters are fixed for all invocations anywhere in the expression tree
(defn generate-expression
  ([max-depth input-image-files]
  ; (let [my-zeroary-ops (vec (concat (zeroary-ops-makers) (vec (map make-read input-image-files))))]
    (let [my-zeroary-ops (vec (map make-read input-image-files))]
      (foo max-depth my-zeroary-ops)))
  ([max-depth]
    (foo max-depth (zeroary-ops-makers))))


(defn generate-random-image-file
  ([uri max-depth input-files]
  (let [expression (generate-expression max-depth input-files)]
    (println expression)
    (save-image expression uri)))
  ([uri max-depth]
  (let [expression (generate-expression max-depth)]
    (println expression)
    (save-image expression uri))))