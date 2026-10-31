(ns heraldicon.util.walk-test
  (:require
   [cljs.test :refer-macros [are deftest is]]
   [clojure.walk :as walk]
   [heraldicon.util.walk :as util.walk]))

(def ^:private charge-like
  [:g {:fill "#ff0000" :stroke "#000000" :clip-path "url(#a)"}
   [:path {:d "M0,0 L10,10" :fill "#ff0000"}]
   (list [:circle {:r 2 :fill "#00ff00"}] [:rect {:width 1}])
   (map identity [[:ellipse {:rx 1 :fill "#ff0000"}]])
   #{:a :b}
   nil
   [:defs [:linearGradient {:id "a"} [:stop {:stop-color "#ff0000"}]]]])

(defn- recolour [value]
  (if (and (vector? value)
           (#{:fill :stop-color} (first value))
           (= (second value) "#ff0000"))
    [(first value) "#0000ff"]
    value))

(defn- drop-clip-path [value]
  (if (map? value)
    (into {} (remove (comp #{:clip-path} key)) value)
    value))

(defn- drop-entry [value]
  (if (and (vector? value) (= (first value) :stroke))
    nil
    value))

(defn- rename-key [value]
  (if (= value :rx) :ry value))

(deftest same-results-as-clojure-walk
  (are [f] (= (util.walk/postwalk f charge-like) (walk/postwalk f charge-like))
    identity
    recolour
    drop-clip-path
    drop-entry
    rename-key))

(deftest renamed-keys-keep-their-order
  (let [m (array-map :a 1 :rx 2 :c 3)]
    (is (= (keys (util.walk/postwalk rename-key m)) (keys (walk/postwalk rename-key m))))))

(deftest unchanged-nodes-are-reused
  (is (identical? (util.walk/postwalk identity charge-like) charge-like))
  (let [result (util.walk/postwalk recolour charge-like)
        path-unchanged [:path {:d "M0,0" :stroke "#000000"}]
        tree [:g path-unchanged [:path {:fill "#ff0000"}]]
        recoloured (util.walk/postwalk recolour tree)]
    (is (not (identical? result charge-like)))
    (is (identical? (nth recoloured 1) path-unchanged))))
