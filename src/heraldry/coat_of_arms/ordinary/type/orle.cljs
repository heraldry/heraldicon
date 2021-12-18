(ns heraldry.coat-of-arms.ordinary.type.orle
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/orle)

(defmethod ordinary-interface/display-name ordinary-type [_] (string "Orle"))

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
                 :ui {:label (string "Thickness")
                      :step 0.1}}
     :distance {:type :range
                :min 0.1
                :max 30
                :default 5
                :ui {:label (string "Distance")
                     :step 0.1}}
     :corner-radius {:type :range
                     :min 0
                     :max 20
                     :default (if (and (= line-type :straight)
                                       (= opposite-line-type :straight))
                                0
                                5)
                     :ui {:label (string "Corner radius")
                          :step 0.1}}
     :smoothness {:type :range
                  :min 0
                  :max 20
                  :default 0
                  :ui {:label (string "Smoothness")
                       :tooltip (string "This might smooth out some remaining corners, best used together with corner radius.")
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

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        distance (interface/get-sanitized-data (c/++ context :distance))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothness (interface/get-sanitized-data (c/++ context :smoothness))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        line-type (interface/get-sanitized-data (c/++ context :line :type))
        opposite-line-type (interface/get-sanitized-data (c/++ context :opposite-line :type))
        points (:points environment)
        width (:width environment)
        distance ((util/percent-of width) distance)
        thickness ((util/percent-of width) thickness)
        environment-shape (environment/effective-shape environment)
        outer-shape (environment/shrink-shape environment-shape distance :round)
        outer-shape (cond-> (path/round-corners outer-shape corner-radius smoothness)
                      (not= line-type :straight) (line/modify-path (interface/get-sanitized-data
                                                                    (c/++ context :line))
                                                                   environment
                                                                   :outer-shape? true))
        inner-shape (environment/shrink-shape environment-shape (+ distance thickness) :round)
        inner-shape (cond-> (path/round-corners inner-shape corner-radius smoothness)
                      (not= opposite-line-type :straight) (line/modify-path (interface/get-sanitized-data
                                                                             (c/++ context :opposite-line))
                                                                            environment))
        part [{:paths [outer-shape
                       inner-shape]}
              [(:top-left points)
               (:bottom-right points)]]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (when outline?
       [:g (outline/style context)
        [:path {:d outer-shape}]
        [:path {:d inner-shape}]])]))
