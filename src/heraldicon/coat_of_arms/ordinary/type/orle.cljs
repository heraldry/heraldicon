(ns heraldicon.coat-of-arms.ordinary.type.orle
  (:require
   [heraldicon.coat-of-arms.field.environment :as environment]
   [heraldicon.coat-of-arms.field.shared :as field.shared]
   [heraldicon.coat-of-arms.line.core :as line]
   [heraldicon.coat-of-arms.ordinary.interface :as ordinary.interface]
   [heraldicon.coat-of-arms.outline :as outline]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.svg.path :as path]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

(def ordinary-type :heraldry.ordinary.type/orle)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/orle)

(defmethod interface/options ordinary-type [context]
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
    {:thickness {:type :range
                 :min 0.1
                 :max 20
                 :default 5
                 :ui {:label :string.option/thickness
                      :step 0.1}}
     :distance {:type :range
                :min 0.1
                :max 30
                :default 4
                :ui {:label :string.option/distance
                     :step 0.1}}
     :corner-radius {:type :range
                     :min 0
                     :max 20
                     :default (if (and (= line-type :straight)
                                       (= opposite-line-type :straight))
                                0
                                5)
                     :ui {:label :string.option/corner-radius
                          :step 0.1}}
     :smoothing {:type :range
                 :min 0
                 :max 20
                 :default 0
                 :ui {:label :string.option/smoothing
                      :tooltip :string.tooltip/smoothing
                      :step 0.1}}
     :line (adjust-line-style
            (line/options (c/++ context :line)
                          :fimbriation? false
                          :corner-dampening? true))
     :opposite-line (adjust-line-style
                     (line/options (c/++ context :opposite-line)
                                   :fimbriation? false
                                   :corner-dampening? true))
     :outline? options/plain-outline?-option}))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        distance (interface/get-sanitized-data (c/++ context :distance))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        line-type (interface/get-sanitized-data (c/++ context :line :type))
        opposite-line-type (interface/get-sanitized-data (c/++ context :opposite-line :type))
        points (:points environment)
        width (:width environment)
        distance ((util/percent-of width) distance)
        thickness ((util/percent-of width) thickness)
        environment-shape (-> environment
                              (update-in [:shape :paths] (partial take 1))
                              environment/effective-shape)
        outer-shape (environment/shrink-shape environment-shape distance :round)
        outer-shape (cond-> (path/round-corners outer-shape corner-radius smoothing)
                      (not= opposite-line-type :straight) (line/modify-path (interface/get-sanitized-data
                                                                             (c/++ context :opposite-line))
                                                                            environment
                                                                            :outer-shape? true))
        inner-shape (environment/shrink-shape environment-shape (+ distance thickness) :round)
        inner-shape (cond-> (path/round-corners inner-shape corner-radius smoothing)
                      (not= line-type :straight) (line/modify-path (interface/get-sanitized-data
                                                                    (c/++ context :line))
                                                                   environment))
        part [{:paths [outer-shape
                       inner-shape]}
              [(:top-left points)
               (:bottom-right points)]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (when outline?
       [:g (outline/style context)
        [:path {:d outer-shape}]
        [:path {:d inner-shape}]])]))
