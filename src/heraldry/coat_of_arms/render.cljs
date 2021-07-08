(ns heraldry.coat-of-arms.render
  (:require [heraldry.coat-of-arms.core :as coat-of-arms]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.render-options :as render-options]
            [heraldry.util :as util]))

(defn coat-of-arms [coat-of-arms width
                    {:keys
                     [render-options
                      svg-export?
                      metadata
                      texture-link] :as context}]
  (let [sanitized-coat-of-arms (options/sanitize coat-of-arms (coat-of-arms/options coat-of-arms))
        sanitized-render-options (options/sanitize render-options (render-options/options render-options))
        escutcheon (if (-> sanitized-render-options
                           :escutcheon-override
                           (not= :none))
                     (:escutcheon-override sanitized-render-options)
                     (:escutcheon sanitized-coat-of-arms))
        shield (escutcheon/field escutcheon)
        environment (-> (environment/transform-to-width shield width)
                        (cond->
                         (:squiggly? sanitized-render-options) (update :shape svg/squiggly-path)))
        field (:field coat-of-arms)
        mask-id (util/id "mask")
        texture (-> sanitized-render-options
                    :texture
                    (#(when (not= % :none) %)))
        texture-displacement? (:texture-displacement? sanitized-render-options)
        texture-id (util/id "texture")
        shiny-id (util/id "shiny")
        use-texture? (or texture-link texture)]
    {:environment environment
     :result [:g
              metadata
              [:defs
               (when (:shiny? sanitized-render-options)
                 [:filter {:id shiny-id}
                  [:feDiffuseLighting {:in "SourceGraphic"
                                       :result "light"
                                       :lighting-color "white"}
                   [:fePointLight {:x 75
                                   :y 20
                                   :z 20}]]
                  [:feComposite {:in "SourceGraphic"
                                 :in2 "light"
                                 :operator "arithmetic"
                                 :k1 1
                                 :k2 0
                                 :k3 0
                                 :k4 0}]])
               (when use-texture?
                 [:filter {:id texture-id}
                  [:feImage {:href (or texture-link (get texture/paths texture))
                             :x 0
                             :y 0
                             :width 150
                             :height 150
                             :preserveAspectRatio "none"
                             :result "image"}]
                  (when texture-displacement?
                    [:feDisplacementMap {:in "SourceGraphic"
                                         :in2 "image"
                                         :scale (get texture/displacements texture)
                                         :xChannelSelector "R"
                                         :yChannelSelector "R"
                                         :result "displaced"}])
                  [:feComposite {:in (if texture-displacement?
                                       "displaced"
                                       "SourceGraphic")
                                 :in2 "image"
                                 :operator "arithmetic"
                                 :k1 1
                                 :k2 0
                                 :k3 0
                                 :k4 0}]])
               (when-not svg-export?
                 filter/shadow)
               [tincture/patterns sanitized-render-options]
               (when (-> sanitized-render-options
                         :mode
                         (= :hatching))
                 hatching/patterns)]
              [:defs
               [(if svg-export?
                  :mask
                  :clipPath)
                {:id mask-id}
                [:path {:d (:shape environment)
                        :fill "#fff"
                        :stroke "none"}]]]
              [:g {(if svg-export?
                     :mask
                     :clip-path) (str "url(#" mask-id ")")}
               [:g {:filter (when use-texture? (str "url(#" texture-id ")"))}
                [:g {:filter (when (:shiny? sanitized-render-options)
                               (str "url(#" shiny-id ")"))}
                 [:path {:d (:shape environment)
                         :fill "#f0f0f0"}]
                 [field/render field environment (-> context
                                                     (dissoc :metadata)
                                                     (update :db-path conj :field)
                                                     (assoc :root-escutcheon escutcheon))]]]]
              (when (or (:escutcheon-outline? sanitized-render-options)
                        (:outline? sanitized-render-options))
                [:g outline/style
                 [:path {:d (:shape environment)}]])]}))
