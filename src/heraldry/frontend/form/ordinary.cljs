(ns heraldry.frontend.form.ordinary
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.form.geometry :as geometry]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn ordinary-type-choice [path key display-name & {:keys [current]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture :argent
                                   :components [{:type key
                                                 :line (when (= key :heraldry.ordinary.type/gore)
                                                         {:type :enarched
                                                          :flipped? true})
                                                 :origin (case key
                                                           :heraldry.ordinary.type/label {:alignment :left}
                                                           nil)
                                                 :geometry (case key
                                                             :heraldry.ordinary.type/label {:width 75
                                                                                            :size 12
                                                                                            :thickness 20}
                                                             :heraldry.ordinary.type/pile {:stretch 0.85}
                                                             nil)
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture (if (= current key) :or :azure)}}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-ordinary-type [path]
  (let [ordinary-type @(rf/subscribe [:get (conj path :type)])]
    [:div.setting
     [:label "Type"]
     " "
     [element/submenu path "Select Ordinary" (get ordinary/ordinary-map ordinary-type) {:min-width "17.5em"}
      (for [[display-name key] ordinary/choices]
        ^{:key key}
        [ordinary-type-choice path key display-name :current ordinary-type])]]))

(defn form [path & {:keys [parent-field form-for-field]}]
  (let [ordinary @(rf/subscribe [:get path])]
    [element/component
     path :ordinary (-> ordinary :type util/translate-cap-first) nil
     [:div.settings
      [form-for-ordinary-type path]
      (let [ordinary-options (ordinary-options/options ordinary)]
        [:<>
         (when (:line ordinary-options)
           [line/form (conj path :line) :options (:line ordinary-options)])
         (when (:opposite-line ordinary-options)
           [line/form (conj path :opposite-line)
            :options (:opposite-line ordinary-options)
            :defaults (options/sanitize (:line ordinary) (:line ordinary-options))
            :title "Opposite Line"])
         (when (:escutcheon ordinary-options)
           [escutcheon/form (conj path :escutcheon)])
         (when (:variant ordinary-options)
           [element/select (conj path :variant)
            "Variant"
            (-> ordinary-options :variant :choices)])
         (when (-> ordinary-options :num-points)
           [element/range-input-with-checkbox (conj path :num-points) "Points"
            (-> ordinary-options :num-points :min)
            (-> ordinary-options :num-points :max)
            :default (options/get-value (:num-points ordinary) (:num-points ordinary-options))])
         (when (:origin ordinary-options)
           [position/form (conj path :origin)
            :title "Origin"
            :options (:origin ordinary-options)])
         (when (:anchor ordinary-options)
           [position/form (conj path :anchor)
            :title "Anchor"
            :options (:anchor ordinary-options)])
         (when (:geometry ordinary-options)
           [geometry/form (conj path :geometry)
            (:geometry ordinary-options)
            :current (:geometry ordinary)])
         (when (:fimbriation ordinary-options)
           [line/form-for-fimbriation (conj path :fimbriation) (:fimbriation ordinary-options)])])
      [element/checkbox (conj path :hints :outline?) "Outline"]]
     [form-for-field (conj path :field) :parent-field parent-field]]))
