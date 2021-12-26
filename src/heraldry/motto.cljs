(ns heraldry.motto
  (:require
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.ribbon :as ribbon]))

(def tinctures-without-furs
  (-> tincture/choices
      (update 0 #(filterv (fn [v]
                            (or (not (vector? v))
                                (-> v second (not= :none))))
                          %))
      (->> (filterv #(when (-> % first :en (not= "Fur")) %)))))

(def type-option
  {:type :choice
   :choices [[(string "Motto") :heraldry.motto.type/motto]
             [(string "Slogan") :heraldry.motto.type/slogan]]
   :ui {:label (string "Type")}})

(defmethod interface/options-subscriptions :heraldry.component/motto [_context]
  #{[:type]
    [:ribbon-variant]})

(defmethod interface/options :heraldry.component/motto [context]
  (let [ribbon-variant (interface/get-raw-data (c/++ context :ribbon-variant))
        motto-type (interface/get-raw-data (c/++ context :type))]
    (-> {:origin {:point {:type :choice
                          :choices [[(string "Top") :top]
                                    [(string "Bottom") :bottom]]
                          :default (case motto-type
                                     :heraldry.motto.type/slogan :top
                                     :bottom)
                          :ui {:label (string "Point")}}
                  :offset-x {:type :range
                             :min -100
                             :max 100
                             :default 0
                             :ui {:label (string "Offset x")
                                  :step 0.1}}
                  :offset-y {:type :range
                             :min -100
                             :max 100
                             :default 0
                             :ui {:label (string "Offset y")
                                  :step 0.1}}
                  :ui {:label (string "Origin")
                       :form-type :position}}
         :geometry (-> geometry/default-options
                       (select-keys [:size :ui])
                       (assoc-in [:size :min] 5)
                       (assoc-in [:size :max] 300)
                       (assoc-in [:size :default] 100))
         :ribbon-variant {:ui {:label (string "Ribbon")
                               :form-type :ribbon-reference-select}}

         :tincture-foreground {:type :choice
                               :choices tinctures-without-furs
                               :default :argent
                               :ui {:label (ribbon/segment-type-map :heraldry.ribbon.segment/foreground)
                                    :form-type :tincture-select}}

         :tincture-background {:type :choice
                               :choices (assoc tinctures-without-furs 0 [(string "Other / Metal")
                                                                         [(string "Darkened foreground") :none]
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
