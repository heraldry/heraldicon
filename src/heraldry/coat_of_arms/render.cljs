(ns heraldry.coat-of-arms.render
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field :as field]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.coat-of-arms.util :as util]))

(defn coat-of-arms [coat-of-arms render-options & {:keys [db-path width] :or {width 100}}]
  (let [shield      (escutcheon/field (:escutcheon coat-of-arms))
        environment (field-environment/transform-to-width shield width)
        field       (:field coat-of-arms)
        mask-id     (util/id "mask")]
    [:g
     [:defs
      filter/shadow
      filter/shiny
      filter/glow
      tincture/patterns
      hatching/patterns]
     [:defs
      [:clipPath
       {:id mask-id}
       [:path {:d      (:shape environment)
               :fill   "#fff"
               :stroke "none"}]]]
     [:g {:clip-path (str "url(#" mask-id ")")}
      [:path {:d    (:shape environment)
              :fill "#f0f0f0"}]
      [field/render field environment render-options :db-path (conj db-path :field)]]
     (when (:outline? render-options)
       [:path.outline {:d (:shape environment)}])]))
