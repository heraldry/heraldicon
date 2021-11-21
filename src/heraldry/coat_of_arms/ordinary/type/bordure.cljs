(ns heraldry.coat-of-arms.ordinary.type.bordure
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Bordure"
                                                              :de "Schildbord"})

(defmethod interface/options ordinary-type [_context]
  {:thickness {:type :range
               :min 0.1
               :max 35
               :default 10
               :ui {:label strings/thickness
                    :step 0.1}}
   :outline? options/plain-outline?-option})

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        width (:width environment)
        thickness ((util/percent-of width) thickness)
        environment-shape (environment/effective-shape environment)
        bordure-shape (environment/shrink-shape environment-shape thickness)
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
