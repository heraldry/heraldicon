(ns or.coad.render
  (:require [or.coad.escutcheon :as escutcheon]
            [or.coad.field :as field]
            [or.coad.field-environment :as field-environment]
            [or.coad.util :as util]))

(defn coat-of-arms [coat-of-arms render-options & {:keys [db-path width] :or {width 100}}]
  (let [shield (escutcheon/field (:escutcheon coat-of-arms))
        environment (field-environment/transform-to-width shield width)
        field (:field coat-of-arms)
        mask-id (util/id "mask")]
    [:g
     [:defs
      [:clipPath
       {:id mask-id}
       [:path {:d (:shape environment)
               :fill "#fff"
               :stroke "none"}]]]
     [:g {:clip-path (str "url(#" mask-id ")")}
      [:path {:d (:shape environment)
              :fill "#f0f0f0"}]
      [field/render field environment render-options :db-path (conj db-path :field)]]
     (when (:outline? render-options)
       [:path.outline {:d (:shape environment)}])]))
