(ns heraldicon.heraldry.ordinary.type.bordure
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bordure)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-type (or (interface/get-raw-data (c/++ context :line :type))
                      :straight)]
    {:thickness {:type :option.type/range
                 :min 0.1
                 :max 35
                 :default 12
                 :ui/label :string.option/thickness
                 :ui/step 0.1}
     :corner-radius {:type :option.type/range
                     :min 0
                     :max 20
                     :default (case line-type
                                :straight 0
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
     :line (-> (line/options (c/++ context :line)
                             :corner-dampening? true)
               (dissoc :flipped?)
               (update-in [:type :choices]
                          (fn [choices]
                            (into []
                                  (remove (fn [[_ line-type]]
                                            (and (not= line-type :straight)
                                                 (-> line-type line/kinds-pattern-map :full?))))
                                  choices))))
     :outline? options/plain-outline?-option}))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        percentage-base (:width parent-environment)
        parent-shape (interface/get-exact-parent-shape context)
        line-length percentage-base
        thickness (math/percent-of percentage-base thickness)
        edge (-> parent-shape
                 (environment/shrink-shape thickness :round)
                 (path/round-corners corner-radius smoothing))]
    (post-process/properties
     {:type ordinary-type
      :edge edge
      :line-length line-length
      :percentage-base percentage-base}
     context)))

(defmethod interface/environment ordinary-type [context _properties]
  (interface/get-parent-environment context))

(defmethod interface/render-shape ordinary-type [context {:keys [edge line]}]
  (let [parent-environment (interface/get-parent-environment context)
        parent-shape (interface/get-exact-parent-shape context)
        shape (cond-> edge
                (not= (:type line) :straight) (line/modify-path line parent-environment))]
    {:shape [parent-shape shape]
     :lines [{:edge-paths [shape]}]}))

(defmethod cottising/cottise-properties ordinary-type [_context _properties]
  nil)
