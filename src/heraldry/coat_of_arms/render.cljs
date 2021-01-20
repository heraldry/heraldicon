(ns heraldry.coat-of-arms.render
  (:require [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field :as field]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.util :as util]))

(defn coat-of-arms [coat-of-arms width {:keys [render-options svg-export?] :as context}]
  (let [shield (escutcheon/field (:escutcheon coat-of-arms))
        environment (field-environment/transform-to-width shield width)
        field (:field coat-of-arms)
        mask-id (util/id "mask")]
    {:environment environment
     :result [:g
              [:defs
               (when-not svg-export?
                 filter/shadow)
               tincture/patterns
               hatching/patterns]
              [:defs
               [:clipPath
                {:id mask-id}
                [:path {:d (:shape environment)
                        :fill "#fff"
                        :stroke "none"}]]]
              [:g {:clip-path (str "url(#" mask-id ")")}
               [:path {:d (:shape environment)
                       :fill "#f0f0f0"}]
               [field/render field environment (-> context
                                                   (update :db-path conj :field))]]
              (when (:outline? render-options)
                [:g division/outline-style
                 [:path {:d (:shape environment)}]])]}))
