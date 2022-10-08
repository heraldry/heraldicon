(ns heraldicon.heraldry.ordinary.type.orle
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.auto-arrange :as auto-arrange]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/orle)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/orle)

(def ^:private positioning-mode-choices
  [[:string.option.positioning-mode-choice/automatic :auto]
   [:string.option.positioning-mode-choice/manual :manual]])

(def positioning-mode-map
  (options/choices->map positioning-mode-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [{:keys [default-size
                default-spacing
                affected-paths]} (interface/get-auto-ordinary-info ordinary-type (interface/parent context))
        auto-positioned? (get affected-paths (:path context))
        line-type (or (interface/get-raw-data (c/++ context :line :type))
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
                               (options/override-if-exists [:base-line :default] :bottom))
        default-thickness (if auto-positioned?
                            default-size
                            5)
        default-distance (if auto-positioned?
                           default-spacing
                           4)]
    {:positioning-mode {:type :option.type/choice
                        :choices positioning-mode-choices
                        :default :auto
                        :ui/label :string.option/positioning-mode
                        :ui/element :ui.element/radio-select}
     :thickness {:type :option.type/range
                 :min 0.1
                 :max 20
                 :default default-thickness
                 :ui/label :string.option/thickness
                 :ui/step 0.1}
     :distance {:type :option.type/range
                :min 0.1
                :max 30
                :default default-distance
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
                          :corner-damping? true))
     :opposite-line (adjust-line-style
                     (line/options (c/++ context :opposite-line)
                                   :fimbriation? false
                                   :corner-damping? true))
     :outline? options/plain-outline?-option}))

(defn- add-orle [{:keys [current-distance]
                  :as arrangement}
                 {:keys [thickness
                         distance
                         line
                         opposite-line]
                  :as orle}]
  (let [line-height (:effective-height line)
        opposite-line-height (:effective-height opposite-line)
        new-current-distance (+ current-distance
                                distance
                                opposite-line-height
                                thickness
                                line-height)]
    (-> arrangement
        (update :orles conj (assoc orle :distance (- new-current-distance
                                                     thickness
                                                     line-height)))
        (assoc :current-distance new-current-distance))))

(defmethod interface/auto-arrangement ordinary-type [_ordinary-type context]
  (let [{:keys [ordinary-contexts
                num-ordinaries
                default-spacing]} (interface/get-auto-ordinary-info ordinary-type context)
        auto-positioned? (> num-ordinaries 1)]
    (if auto-positioned?
      (let [{:keys [width height]} (interface/get-parent-field-environment (first ordinary-contexts))
            percentage-base (min width height)
            apply-percentage (partial math/percent-of percentage-base)
            orles (let [{:keys [orles]} (->> ordinary-contexts
                                             (map (fn [context]
                                                    (-> {:context context
                                                         :line-length percentage-base
                                                         :percentage-base percentage-base}
                                                        auto-arrange/set-distance
                                                        auto-arrange/set-thickness
                                                        auto-arrange/set-line-data
                                                        (update :distance apply-percentage)
                                                        (update :thickness apply-percentage))))
                                             (reduce add-orle {:current-distance 0
                                                               :orles []}))]
                    orles)]
        {:arrangement-data (into {}
                                 (map (fn [{:keys [context]
                                            :as orle}]
                                        [(:path context) orle]))
                                 orles)
         :num-ordinaries num-ordinaries})
      {:arrangement-data {}
       :num-ordinaries num-ordinaries})))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]} (interface/get-parent-field-environment context)
        percentage-base (min width height)
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [arrangement-data]} (interface/get-auto-arrangement ordinary-type (interface/parent context))
        {arranged-thickness :thickness
         arranged-distance :distance} (get arrangement-data (:path context))
        distance (or arranged-distance
                     (apply-percentage
                      (interface/get-sanitized-data (c/++ context :distance))))
        thickness (or arranged-thickness
                      (apply-percentage
                       (interface/get-sanitized-data (c/++ context :thickness))))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        parent-shape (interface/get-parent-field-shape context)
        line-length percentage-base
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
  (interface/get-parent-field-environment context))

(defmethod interface/render-shape ordinary-type [context {:keys [outer-edge inner-edge line opposite-line]}]
  (let [parent-environment (interface/get-parent-field-environment context)
        outer-shape (cond-> outer-edge
                      (not= (:type opposite-line) :straight) (line/modify-path opposite-line parent-environment
                                                                               :outer-shape? true))
        inner-shape (cond-> inner-edge
                      (not= (:type line) :straight) (line/modify-path line parent-environment))]
    {:shape [outer-shape inner-shape]
     :edges [{:paths [outer-shape inner-shape]}]}))

(defmethod cottising/cottise-properties ordinary-type [_context _properties]
  nil)

(defmethod interface/parent-field-environment ordinary-type [context]
  (interface/get-environment (interface/parent context)))

(prefer-method interface/parent-field-environment ordinary-type :heraldry/ordinary)

(defmethod interface/parent-field-shape ordinary-type [context]
  (interface/get-exact-shape (interface/parent context)))

(prefer-method interface/parent-field-shape ordinary-type :heraldry/ordinary)
