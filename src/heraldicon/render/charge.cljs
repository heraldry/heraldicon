(ns heraldicon.render.charge
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]))

(defn render-standalone [{:keys [svg-export?
                                 target-width
                                 target-height
                                 embed-fonts] :as context}]
  (let [shield (escutcheon/field :rectangle nil nil nil nil nil)
        environment (environment/transform-to-width shield 100)
        [result-width result-height] [(:width environment)
                                      (:height environment)]
        target-width (or target-width 1000)
        [document-width
         document-height
         document-scale] (if (and svg-export?
                                  (or target-width
                                      target-height))
                           (let [scale-width (when target-width
                                               (/ target-width result-width))
                                 scale-height (when target-height
                                                (/ target-height result-height))
                                 scale (or (when (and scale-width scale-height)
                                             (min scale-width scale-height))
                                           scale-width
                                           scale-height)]
                             [(* result-width scale)
                              (* result-height scale)
                              scale])
                           [result-width
                            result-height
                            1])
        used-fonts (into #{}
                         (keep (fn [j]
                                 (interface/get-sanitized-data (c/++ context :segments j :font))))
                         (range (interface/get-list-size (c/++ context :segments))))]
    [:svg (merge
           {:viewBox (str "0 0 " document-width " " document-height)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when (and svg-export?
                embed-fonts)
       [embed-fonts used-fonts])
     [:g {:transform (str "scale(" document-scale "," document-scale ")")}
      [interface/render-component (assoc context :environment environment)]]]))
