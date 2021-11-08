(ns heraldry.motto
  (:require
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.interface :as interface]
   [heraldry.ribbon :as ribbon]
   [heraldry.strings :as strings]))

(def tinctures-without-furs
  (-> tincture/choices
      (update 0 #(filterv (fn [v]
                            (or (not (vector? v))
                                (-> v second (not= :none))))
                          %))
      (->> (filterv #(when (-> % first :en (not= "Fur")) %)))))

(def default-motto-options
  {:type {:type :choice
          :choices [[strings/motto :heraldry.motto.type/motto]
                    [strings/slogan :heraldry.motto.type/slogan]]
          :ui {:label strings/type}}
   :origin (-> position/default-options
               (assoc-in [:point :choices] [[strings/top :top]
                                            [strings/bottom :bottom]])
               (assoc-in [:point :default] :bottom)
               (assoc-in [:offset-x :min] -100)
               (assoc-in [:offset-x :max] 100)
               (assoc-in [:offset-y :min] -100)
               (assoc-in [:offset-y :max] 100)
               (dissoc :alignment)
               (assoc-in [:ui :label] strings/origin))
   :geometry (-> geometry/default-options
                 (select-keys [:size :ui])
                 (assoc-in [:size :min] 5)
                 (assoc-in [:size :max] 300)
                 (assoc-in [:size :default] 100))
   :ribbon-variant {:ui {:label strings/ribbon
                         :form-type :ribbon-reference-select}}
   :ribbon (ribbon/options nil)

   :tincture-foreground {:type :choice
                         :choices tinctures-without-furs
                         :default :argent
                         :ui {:label (ribbon/segment-type-map :heraldry.ribbon.segment/foreground)
                              :form-type :tincture-select}}

   :tincture-background {:type :choice
                         :choices (assoc tinctures-without-furs 0 [{:en "Other / Metal"
                                                                    :de "Andere / Metall"}
                                                                   [{:en "Darkened foreground"
                                                                     :de "Vorderseite verdunkelt"} :none]
                                                                   [(tincture/tincture-map :argent) :argent]
                                                                   [(tincture/tincture-map :or) :or]])
                         :default :none
                         :ui {:label (ribbon/segment-type-map :heraldry.ribbon.segment/background)
                              :form-type :tincture-select}}

   :tincture-text {:type :choice
                   :choices tinctures-without-furs
                   :default :helmet-dark
                   :ui {:label (ribbon/segment-type-map :heraldry.ribbon.segment/foreground-with-text)
                        :form-type :tincture-select}}})

(defn motto-options [data]
  (let [ribbon-variant? (:ribbon-variant data)
        motto-type (:type data)]
    (-> default-motto-options
        (cond->
          ribbon-variant? (update :ribbon ribbon/options (:ribbon data))
          (not ribbon-variant?) (dissoc :ribbon)
          (= motto-type :heraldry.motto.type/slogan) (assoc-in [:origin :point :default] :top))
        (update :origin position/adjust-options))))

(defmethod interface/component-options :heraldry.component/motto [context]
  (motto-options (interface/get-raw-data context)))
