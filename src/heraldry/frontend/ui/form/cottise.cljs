(ns heraldry.frontend.ui.form.cottise
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:line
    :opposite-line
    :distance
    :thickness
    :outline?]))

;; TODO: context
(defn cottise-name [{:keys [path] :as context}]
  (let [cottise-key (last path)
        ordinary-type (interface/get-raw-data (-> context
                                                  (c/-- 2)
                                                  (c/++ :type)))]
    (util/str-tr
     strings/cottise
     " "
     (-> (cond
           (#{:heraldry.ordinary.type/pale}
            ordinary-type) {:cottise-1 (util/str-tr "1 " strings/left)
                            :cottise-2 (util/str-tr "2 " strings/left)
                            :cottise-opposite-1 (util/str-tr "1 " strings/right)
                            :cottise-opposite-2 (util/str-tr "2 " strings/right)}
           (#{:heraldry.ordinary.type/fess
              :heraldry.ordinary.type/bend
              :heraldry.ordinary.type/bend-sinister
              :heraldry.ordinary.type/chevron}
            ordinary-type) {:cottise-1 (util/str-tr "1 " strings/top)
                            :cottise-2 (util/str-tr "2 " strings/top)
                            :cottise-opposite-1 (util/str-tr "1 " strings/bottom)
                            :cottise-opposite-2 (util/str-tr "2 " strings/bottom)}
           :else {:cottise-1 "1"
                  :cottise-2 "2"
                  :cottise-opposite-1 (util/str-tr "1 " strings/opposite)
                  :cottise-opposite-2 (util/str-tr "2 " strings/opposite)
                  :cottise-extra-1 (util/str-tr "1 " strings/extra)
                  :cottise-extra-2 (util/str-tr "2 " strings/extra)})
         (get cottise-key)))))

(defmethod ui-interface/component-node-data :heraldry.component/cottise [context]
  {:title (cottise-name context)
   :validation @(rf/subscribe [:validate-cottise context])
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/cottise [_context]
  {:form form})
