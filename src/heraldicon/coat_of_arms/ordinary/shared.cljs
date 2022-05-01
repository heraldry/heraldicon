(ns heraldicon.coat-of-arms.ordinary.shared
  (:require
   [clojure.string :as s]
   [heraldicon.coat-of-arms.ordinary.humetty :as humetty]
   [heraldicon.coat-of-arms.ordinary.voided :as voided]
   [heraldicon.coat-of-arms.outline :as outline]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.svg.path :as path]))

(defn add-humetty-and-voided [options context]
  (let [humetty? (interface/get-raw-data (c/++ context :humetty :humetty?))]
    (-> options
        (assoc :voided (voided/options (c/++ context :voided))
               :humetty (humetty/options (c/++ context :humetty)))
        (cond->
          humetty? (->
                     (update :line dissoc :fimbriation)
                     (update :opposite-line dissoc :fimbriation)
                     (update :extra-line dissoc :fimbriation)
                     (dissoc :fimbriation))))))

(defn adjust-shape [shape base-width base-thickness context]
  (let [voided? (interface/get-sanitized-data (c/++ context :voided :voided?))
        humetty? (interface/get-sanitized-data (c/++ context :humetty :humetty?))]
    (-> shape
        path/make-path
        (cond->
          humetty? (humetty/coup base-width (c/++ context :humetty))
          voided? (voided/void base-thickness (c/++ context :voided))))))

(defn adjusted-shape-outline [shape outline? context line-and-fimbriation]
  (let [voided? (interface/get-sanitized-data (c/++ context :voided :voided?))
        humetty? (interface/get-sanitized-data (c/++ context :humetty :humetty?))]
    [:<>
     (cond
       (and outline?
            humetty?) [:g (outline/style context)
                       [:path {:d (s/join "" (:paths shape))}]]
       (and outline?
            voided?) [:g (outline/style context)
                      [:path {:d (-> shape :paths last)}]]
       :else nil)
     (when-not humetty?
       line-and-fimbriation)]))
