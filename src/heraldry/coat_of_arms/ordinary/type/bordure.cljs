(ns heraldry.coat-of-arms.ordinary.type.bordure
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

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary-interface/display-name ordinary-type [_] (string "Bordure"))

(defmethod interface/options ordinary-type [context]
  (let [line-type (or (interface/get-raw-data (c/++ context :line :type))
                      :straight)]
    {:thickness {:type :range
                 :min 0.1
                 :max 35
                 :default 10
                 :ui {:label (string "Thickness")
                      :step 0.1}}
     :corner-radius {:type :range
                     :min 0
                     :max 20
                     :default (case line-type
                                :straight 0
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
     :line (-> (line/options (c/++ context :line)
                             :fimbriation? false
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

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothness (interface/get-sanitized-data (c/++ context :smoothness))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        line-type (interface/get-sanitized-data (c/++ context :line :type))
        points (:points environment)
        width (:width environment)
        thickness ((util/percent-of width) thickness)
        environment-shape (environment/effective-shape environment)
        bordure-shape (environment/shrink-shape environment-shape thickness :round)
        bordure-shape (cond-> (path/round-corners bordure-shape corner-radius smoothness)
                        (not= line-type :straight) (line/modify-path (c/++ context :line)
                                                                     environment))
        part [{:paths [environment-shape
                       bordure-shape]}
              [(:top-left points)
               (:bottom-right points)]]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (when outline?
       [:g (outline/style context)
        [:path {:d bordure-shape}]])]))
