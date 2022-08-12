(ns heraldicon.heraldry.ordinary.type.orle
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.render :as ordinary.render]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/orle)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/orle)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-type (or (interface/get-raw-data (c/++ context :line :type))
                      :straight)
        opposite-line-type (or (interface/get-raw-data (c/++ context :opposite-line :type))
                               :straight)
        adjust-line-style #(-> %
                               (update-in [:type :choices]
                                          (fn [choices]
                                            (into []
                                                  (remove (fn [[_ line-type]]
                                                            (and (not= line-type :straight)
                                                                 (-> line-type line/kinds-pattern-map :full?))))
                                                  choices)))
                               (options/override-if-exists [:base-line :default] :bottom))]
    {:thickness {:type :option.type/range
                 :min 0.1
                 :max 20
                 :default 5
                 :ui/label :string.option/thickness
                 :ui/step 0.1}
     :distance {:type :option.type/range
                :min 0.1
                :max 30
                :default 4
                :ui/label :string.option/distance
                :ui/step 0.1}
     :corner-radius {:type :option.type/range
                     :min 0
                     :max 20
                     :default (if (and (= line-type :straight)
                                       (= opposite-line-type :straight))
                                0
                                5)
                     :ui/label :string.option/corner-radius
                     :ui/step 0.1}
     :smoothing {:type :option.type/range
                 :min 0
                 :max 20
                 :default 0
                 :ui/label :string.option/smoothing
                 :ui/tooltip :string.tooltip/smoothing
                 :ui/step 0.1}
     :line (adjust-line-style
            (line/options (c/++ context :line)
                          :corner-dampening? true))
     :opposite-line (adjust-line-style
                     (line/options (c/++ context :opposite-line)
                                   :fimbriation? false
                                   :corner-dampening? true))
     :outline? options/plain-outline?-option}))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        percentage-base (:width parent-environment)
        parent-shape (interface/get-exact-parent-shape context)
        line-length percentage-base
        distance (math/percent-of percentage-base distance)
        thickness (math/percent-of percentage-base thickness)
        outer-edge (-> parent-shape
                       (environment/shrink-shape distance :round)
                       (path/round-corners corner-radius smoothing))
        inner-edge (-> parent-shape
                       (environment/shrink-shape (+ distance thickness) :round)
                       (path/round-corners corner-radius smoothing))]
    (post-process/properties
     {:type ordinary-type
      :outer-edge outer-edge
      :inner-edge inner-edge
      :line-length line-length
      :percentage-base percentage-base}
     context)))

(defmethod interface/environment ordinary-type [context _properties]
  (interface/get-parent-environment context))

(defmethod interface/render-shape ordinary-type [context {:keys [outer-edge inner-edge line opposite-line]}]
  (let [parent-environment (interface/get-parent-environment context)
        outer-shape (cond-> outer-edge
                      (not= (:type opposite-line) :straight) (line/modify-path opposite-line parent-environment
                                                                               :outer-shape? true))
        inner-shape (cond-> inner-edge
                      (not= (:type line) :straight) (line/modify-path line parent-environment))]
    {:shape [outer-shape inner-shape]
     :lines [{:edge-paths [outer-shape inner-shape]}]}))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))

(defmethod cottising/cottise-properties ordinary-type [_context _properties]
  nil)
