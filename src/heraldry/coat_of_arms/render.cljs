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
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn coat-of-arms [path width
                    {:keys
                     [render-options
                      svg-export?
                      metadata
                      root-transform
                      texture-link] :as context}]
  (let [mode (options/sanitized-value (conj render-options :mode) context)
        escutcheon-override (options/sanitized-value (conj render-options :escutcheon-override) context)
        escutcheon-shadow? (options/sanitized-value (conj render-options :shadow?) context)
        escutcheon-outline? (options/sanitized-value (conj render-options :outline?) context)
        outline? (options/sanitized-value (conj render-options :outline?) context)
        shiny? (options/sanitized-value (conj render-options :shiny?) context)
        squiggly? (options/sanitized-value (conj render-options :squiggly?) context)
        theme (options/sanitized-value (conj render-options :theme) context)
        texture (options/sanitized-value (conj render-options :texture) context)
        texture-displacement? (options/sanitized-value (conj render-options :texture-displacement?) context)
        escutcheon (options/sanitized-value (conj path :escutcheon) context)
        escutcheon (if (not= escutcheon-override :none)
                     escutcheon-override
                     escutcheon)
        shield (escutcheon/field escutcheon)
        environment (-> (environment/transform-to-width shield width)
                        (cond->
                         squiggly? (update :shape svg/squiggly-path)))
        mask-id (util/id "mask")
        texture (when-not (= texture :none)
                  texture)
        texture-id (util/id "texture")
        shiny-id (util/id "shiny")
        use-texture? (or texture-link texture)]
    {:environment environment
     :result [:g {:filter (when escutcheon-shadow?
                            "url(#shadow)")}
              [:g {:transform root-transform}
               metadata
               [:defs
                (when shiny?
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
                [tincture/patterns theme]
                (when (= mode :hatching)
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
                 [:g {:filter (when shiny?
                                (str "url(#" shiny-id ")"))}
                  [:path {:d (:shape environment)
                          :fill "#f0f0f0"}]
                  [field/render (conj path :field)
                   environment
                   (-> context
                       (dissoc :metadata)
                       (assoc :root-escutcheon escutcheon))]]]]
               (when (or escutcheon-outline?
                         outline?)
                 [:g outline/style
                  [:path {:d (:shape environment)}]])]]}))
