(ns spec.heraldicon.spec-util
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str])
  (:require-macros [spec.heraldicon.spec-util]))

(defn key-in? [m]
  (-> m keys set))

(def non-blank-string?
  (s/and string? (complement str/blank?)))

(def pos-number?
  (s/and number? (complement neg?)))

(defn- specize
  ([s] (or (s/spec? s) (s/specize* s)))
  ([s form] (or (s/spec? s) (s/specize* s form))))

(defn spec-impl
  [form pred]
  (let [lspec (delay (specize pred form))]
    (reify
      s/Specize
      (specize* [s] s)
      (specize* [s _] s)

      s/Spec
      (conform* [_ x] (s/conform* @lspec x))
      (unform* [_ x] (s/unform* @lspec x))
      (explain* [_ path via in x] (s/explain* @lspec path via in x))
      (gen* [_ overrides path rmap] (s/gen* @lspec overrides path rmap))
      (with-gen* [_ gfn] (s/with-gen* @lspec gfn))
      (describe* [_] `(spec ~form)))))
