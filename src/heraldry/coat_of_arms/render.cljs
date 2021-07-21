(ns heraldry.coat-of-arms.render
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(defn coat-of-arms [path width
                    {:keys
                     [svg-export?
                      metadata-path
                      root-transform
                      texture-link] :as context}]
  (let [mode (options/render-option :mode context)
        escutcheon-override (options/render-option :escutcheon-override context)
        escutcheon-shadow? (options/render-option :escutcheon-shadow? context)
        escutcheon-outline? (options/render-option :escutcheon-outline? context)
        outline? (options/render-option :outline? context)
        shiny? (options/render-option :shiny? context)
        squiggly? (options/render-option :squiggly? context)
        theme (options/render-option :theme context)
        texture (options/render-option :texture context)
        texture-displacement? (options/render-option :texture-displacement? context)
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
               (when metadata-path
                 [metadata/attribution metadata-path :arms context])
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
                  [field-shared/render (conj path :field)
                   environment
                   (-> context
                       (dissoc :metadata-path)
                       (assoc :root-escutcheon escutcheon))]]]]
               (when (or escutcheon-outline?
                         outline?)
                 [:g outline/style
                  [:path {:d (:shape environment)}]])]]}))
