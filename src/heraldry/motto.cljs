(ns heraldry.motto
  (:require
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
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

(def type-option
  {:type :choice
   :choices [[strings/motto :heraldry.motto.type/motto]
             [strings/slogan :heraldry.motto.type/slogan]]
   :ui {:label strings/type}})

(defmethod interface/component-options :heraldry.component/motto [context]
  (let [ribbon-variant (interface/get-raw-data (c/++ context :ribbon-variant))
        motto-type (interface/get-raw-data (c/++ context :type))]
    (-> {:origin {:point {:type :choice
                          :choices [[strings/top :top]
                                    [strings/bottom :bottom]]
                          :default (case motto-type
                                     :heraldry.motto.type/slogan :top
                                     :bottom)
                          :ui {:label strings/point}}
                  :offset-x {:type :range
                             :min -100
                             :max 100
                             :default 0
                             :ui {:label strings/offset-x
                                  :step 0.1}}
                  :offset-y {:type :range
                             :min -100
                             :max 100
                             :default 0
                             :ui {:label strings/offset-y
                                  :step 0.1}}
                  :ui {:label strings/origin
                       :form-type :position}}
         :geometry (-> geometry/default-options
                       (select-keys [:size :ui])
                       (assoc-in [:size :min] 5)
                       (assoc-in [:size :max] 300)
                       (assoc-in [:size :default] 100))
         :ribbon-variant {:ui {:label strings/ribbon
                               :form-type :ribbon-reference-select}}

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
                              :form-type :tincture-select}}}
        (cond->
          ribbon-variant (assoc :ribbon (ribbon/options (c/++ context :ribbon))))
        (assoc :type type-option))))
