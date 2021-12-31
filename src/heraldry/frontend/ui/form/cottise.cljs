(ns heraldry.frontend.ui.form.cottise
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.frontend.validation :as validation]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

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
     :string.entity/cottise
     " "
     (-> (cond
           (#{:heraldry.ordinary.type/pale}
            ordinary-type) {:cottise-1 (util/str-tr "1 " :string.miscellaneous/left)
                            :cottise-2 (util/str-tr "2 " :string.miscellaneous/left)
                            :cottise-opposite-1 (util/str-tr "1 " :string.miscellaneous/right)
                            :cottise-opposite-2 (util/str-tr "2 " :string.miscellaneous/right)}
           (#{:heraldry.ordinary.type/fess
              :heraldry.ordinary.type/bend
              :heraldry.ordinary.type/bend-sinister
              :heraldry.ordinary.type/chevron}
            ordinary-type) {:cottise-1 (util/str-tr "1 " :string.miscellaneous/top)
                            :cottise-2 (util/str-tr "2 " :string.miscellaneous/top)
                            :cottise-opposite-1 (util/str-tr "1 " :string.miscellaneous/bottom)
                            :cottise-opposite-2 (util/str-tr "2 " :string.miscellaneous/bottom)}
           :else {:cottise-1 "1"
                  :cottise-2 "2"
                  :cottise-opposite-1 (util/str-tr "1 " :string.miscellaneous/opposite)
                  :cottise-opposite-2 (util/str-tr "2 " :string.miscellaneous/opposite)
                  :cottise-extra-1 (util/str-tr "1 " :string.miscellaneous/extra)
                  :cottise-extra-2 (util/str-tr "2 " :string.miscellaneous/extra)})
         (get cottise-key)))))

(defmethod ui-interface/component-node-data :heraldry.component/cottise [context]
  {:title (cottise-name context)
   :validation (validation/validate-cottise context)
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/cottise [_context]
  {:form form})
