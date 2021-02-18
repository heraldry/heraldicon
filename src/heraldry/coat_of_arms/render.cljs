(ns heraldry.coat-of-arms.render
  (:require [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field :as field]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.util :as util]))

(defn coat-of-arms [coat-of-arms width {:keys [render-options svg-export? metadata] :as context}]
  (let [escutcheon  (if (-> render-options
                            :escutcheon-override
                            (or :none)
                            (not= :none))
                      (:escutcheon-override render-options)
                      (:escutcheon coat-of-arms))
        shield      (escutcheon/field escutcheon)
        environment (-> (field-environment/transform-to-width shield width)
                        (cond->
                            (:squiggly? render-options) (update :shape line/squiggly-path)))
        field       (:field coat-of-arms)
        mask-id     (util/id "mask")]
    {:environment environment
     :result      [:g
                   metadata
                   [:defs
                    (when (:shiny? render-options)
                      [:filter#shiny
                       [:feDiffuseLighting {:in             "SourceGraphic"
                                            :result         "light"
                                            :lighting-color "white"}
                        [:fePointLight {:x 75
                                        :y 20
                                        :z 20}]]
                       [:feComposite {:in       "SourceGraphic"
                                      :in2      "light"
                                      :operator "arithmetic"
                                      :k1       1
                                      :k2       0
                                      :k3       0
                                      :k4       0}]])
                    (when-not svg-export?
                      filter/shadow)
                    [tincture/patterns render-options]
                    (when (-> render-options
                              :mode
                              (= :hatching))
                      hatching/patterns)]
                   [:defs
                    [(if svg-export?
                       :mask
                       :clipPath)
                     {:id mask-id}
                     [:path {:d      (:shape environment)
                             :fill   "#fff"
                             :stroke "none"}]]]
                   [:g {(if svg-export?
                          :mask
                          :clip-path) (str "url(#" mask-id ")")}
                    [:g {:filter (when (:shiny? render-options)
                                   "url(#shiny)")}
                     [:path {:d    (:shape environment)
                             :fill "#f0f0f0"}]
                     [field/render field environment (-> context
                                                         (update :db-path conj :field)
                                                         (assoc :root-escutcheon escutcheon))]]]
                   (when (:outline? render-options)
                     [:g division/outline-style
                      [:path {:d (:shape environment)}]])]}))
