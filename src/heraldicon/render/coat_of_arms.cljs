(ns heraldicon.render.coat-of-arms
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.render :as field.render]
   [heraldicon.interface :as interface]
   [heraldicon.render.hatching :as hatching]
   [heraldicon.render.outline :as outline]
   [heraldicon.render.pattern :as pattern]
   [heraldicon.render.texture :as texture]
   [heraldicon.svg.filter :as filter]
   [heraldicon.svg.metadata :as svg.metadata]
   [heraldicon.svg.squiggly :as squiggly]
   [heraldicon.util.uid :as uid]))

(defn render [{:keys
               [svg-export?
                metadata-path
                texture-link] :as context} width]
  (let [mode (interface/render-option :mode context)
        escutcheon (interface/render-option :escutcheon context)
        escutcheon-shadow? (when-not svg-export?
                             (interface/render-option :escutcheon-shadow? context))
        escutcheon-outline? (interface/render-option :escutcheon-outline? context)
        outline? (interface/render-option :outline? context)
        shiny? (interface/render-option :shiny? context)
        squiggly? (interface/render-option :squiggly? context)
        theme (interface/render-option :theme context)
        texture (interface/render-option :texture context)
        texture-displacement? (interface/render-option :texture-displacement? context)
        flag-width (interface/render-option :flag-width context)
        flag-height (interface/render-option :flag-height context)
        flag-swallow-tail (interface/render-option :flag-swallow-tail context)
        flag-tail-point-height (interface/render-option :flag-tail-point-height context)
        flag-tail-tongue (interface/render-option :flag-tail-tongue context)
        shield (escutcheon/field escutcheon flag-width flag-height flag-swallow-tail
                                 flag-tail-point-height flag-tail-tongue)
        environment (cond-> (environment/transform-to-width shield width)
                      squiggly? (update-in [:shape :paths]
                                           #(into []
                                                  (map squiggly/squiggly-path)
                                                  %)))
        {env-width :width
         env-height :height} environment
        mask-id (uid/generate "mask")
        texture-id (uid/generate "texture")
        shiny-id (uid/generate "shiny")
        texture-link (or texture-link (texture/full-path texture))]
    {:environment environment
     :result [:g {:filter (when escutcheon-shadow?
                            "url(#shadow)")}
              (when metadata-path
                [svg.metadata/attribution (assoc context :path metadata-path) :arms])
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
               (when texture-link
                 [:filter {:id texture-id}
                  [:feImage {:href texture-link
                             :x 0
                             :y 0
                             :width (max env-width env-height)
                             :height (max env-width env-height)
                             :preserveAspectRatio "none"
                             :result "image"}]
                  (when texture-displacement?
                    [:feDisplacementMap {:in "SourceGraphic"
                                         :in2 "image"
                                         :scale (texture/displacement texture)
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
               [pattern/defs theme]
               (when (= mode :hatching)
                 hatching/defs)]
              [:defs
               [(if svg-export?
                  :mask
                  :clipPath)
                {:id mask-id}
                [:path {:d (s/join "" (-> environment :shape :paths))
                        :clip-rule "evenodd"
                        :fill-rule "evenodd"
                        :fill "#fff"
                        :stroke "none"}]]]
              [:g {(if svg-export?
                     :mask
                     :clip-path) (str "url(#" mask-id ")")}
               [:g {:filter (when texture-link (str "url(#" texture-id ")"))}
                [:g {:filter (when shiny?
                               (str "url(#" shiny-id ")"))}
                 [:path {:d (s/join "" (-> environment :shape :paths))
                         :fill-rule "evenodd"
                         :fill "#f0f0f0"}]
                 [field.render/render (-> context
                                          (c/++ :field)
                                          (assoc :environment environment)
                                          (dissoc :metadata-path))]]]]
              (when (or escutcheon-outline?
                        outline?)
                [:g (outline/style context)
                 [:path {:d (s/join "" (-> environment :shape :paths))}]])]}))
