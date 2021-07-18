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

(defn coat-of-arms [coat-of-arms width
                    {:keys
                     [render-options
                      svg-export?
                      metadata
                      root-transform
                      texture-link] :as context}]
  (let [real-render-options (if (vector? render-options)
                              @(rf/subscribe [:get-value render-options])
                              render-options)
        context (-> context
                    (assoc :render-options real-render-options))
        [mode
         escutcheon-override
         escutcheon-shadow?
         escutcheon-outline?
         outline?
         shiny?
         squiggly?
         theme
         texture
         texture-displacement?] (options/effective-values
                                 [[:mode]
                                  [:escutcheon-override]
                                  [:escutcheon-shadow?]
                                  [:escutcheon-outline?]
                                  [:outline?]
                                  [:shiny?]
                                  [:squiggly?]
                                  [:theme]
                                  [:texture]
                                  [:texture-displacement?]] render-options render-options/options)
        sanitized-coat-of-arms (options/sanitize coat-of-arms (coat-of-arms/options coat-of-arms))
        escutcheon (if (not= escutcheon-override :none)
                     escutcheon-override
                     (:escutcheon sanitized-coat-of-arms))
        shield (escutcheon/field escutcheon)
        environment (-> (environment/transform-to-width shield width)
                        (cond->
                         squiggly? (update :shape svg/squiggly-path)))
        field (:field coat-of-arms)
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
                  [field/render field environment (-> context
                                                      (dissoc :metadata)
                                                      (update :db-path conj :field)
                                                      (assoc :root-escutcheon escutcheon))]]]]
               (when (or escutcheon-outline?
                         outline?)
                 [:g outline/style
                  [:path {:d (:shape environment)}]])]]}))
