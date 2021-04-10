(ns heraldry.frontend.form.line
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn line-type-choice [path key display-name & {:keys [current]}]
  (let [options (line/options {:type key})
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :flag
                           :field {:type :heraldry.field.type/per-fess
                                   :line {:type key
                                          :width (case key
                                                   :enarched nil
                                                   (* 2 (options/get-value nil (:width options))))
                                          :height (case key
                                                    :enarched 0.25
                                                    nil)}
                                   :fields [{:type :heraldry.field.type/plain
                                             :tincture :argent}
                                            {:type :heraldry.field.type/plain
                                             :tincture (if (= key current) :or :azure)}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "6.5em"
                    :height "4.5em"}
            :viewBox "0 0 120 80"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-line-type [path & {:keys [options can-disable? default value]}]
  (let [line @(rf/subscribe [:get path])
        value (or value
                  (options/get-value (:type line) (:type options)))]
    [:div.setting
     [:label "Type"]
     [:div.other {:style {:display "inline-block"}}
      (when can-disable?
        [:input {:type "checkbox"
                 :checked (some? (:type line))
                 :on-change #(let [new-checked? (-> % .-target .-checked)]
                               (if new-checked?
                                 (rf/dispatch [:set (conj path :type) default])
                                 (rf/dispatch [:remove (conj path :type)])))}])
      (if (some? (:type line))
        [element/submenu (conj path :type) "Select Line Type" (get line/line-map value) {:min-width "25em"}
         (for [[display-name key] (-> options :type :choices)]
           ^{:key display-name}
           [line-type-choice (conj path :type) key display-name :current value])]
        (when can-disable?
          [:span {:style {:color "#ccc"}} (get line/line-map value)
           " (inherited)"]))]]))

(defn form-for-fimbriation [path options values & {:keys [defaults]}]
  (let [fimbriation @(rf/subscribe [:get path])
        {:keys [mode
                tincture-1
                tincture-2]} (merge fimbriation values)
        link-name (case mode
                    :single (util/combine
                             ", "
                             ["single"
                              (util/translate-cap-first tincture-1)])
                    :double (util/combine
                             ", "
                             ["double"
                              (util/translate-cap-first tincture-2)
                              (util/translate-cap-first tincture-1)])
                    "None")]
    [:div.setting
     [:label "Fimbriation"]
     " "
     [element/submenu path "Fimbriation" link-name {}
      (when (-> options :mode)
        [element/radio-select (conj path :mode) (-> options :mode :choices)
         :default (or (:mode defaults) (-> options :mode :default))])
      (when (#{:single :double} mode)
        [:<>
         (when (-> options :alignment)
           [element/select (conj path :alignment) "Alignment"
            (-> options :alignment :choices)
            :default (or (:alignment defaults) (-> options :alignment :default))])
         (when (-> options :corner)
           [element/select (conj path :corner) "Corner"
            (-> options :corner :choices)
            :default (or (:corner defaults) (-> options :corner :default))])
         (when (-> options :thickness-1)
           [element/range-input (conj path :thickness-1)
            (str "Thickness"
                 (when (#{:double} mode) " 1"))
            (-> options :thickness-1 :min)
            (-> options :thickness-1 :max)
            :default (or (:thickness-1 defaults) (-> options :thickness-1 :default))
            :step 0.1
            :display-function #(str % "%")])
         (when (-> options :tincture-1)
           [tincture/form (conj path :tincture-1)
            :label (str "Tincture"
                        (when (#{:double} mode) " 1"))])
         (when (and (#{:double} mode)
                    (-> options :thickness-2))
           [element/range-input (conj path :thickness-2) "Thickness 2"
            (-> options :thickness-2 :min)
            (-> options :thickness-2 :max)
            :default (or (:thickness-1 defaults) (-> options :thickness-1 :default))
            :step 0.1
            :display-function #(str % "%")])
         (when (and (#{:double} mode)
                    (-> options :tincture-2))
           [tincture/form (conj path :tincture-2)
            :label "Tincture 2"])])]]))

(defn form [path & {:keys [title options defaults] :or {title "Line"}}]
  (let [line @(rf/subscribe [:get path])
        line-type (or (:type line)
                      (:type defaults))
        line-eccentricity (or (:eccentricity line)
                              (:eccentricity defaults))
        line-height (or (:height line)
                        (:height defaults))
        line-width (or (:width line)
                       (:width defaults))
        line-offset (or (:offset line)
                        (:offset defaults))
        fimbriation-tincture-1 (or (-> line :fimbriation :tincture-1)
                                   (-> defaults :fimbriation :tincture-1))
        fimbriation-tincture-2 (or (-> line :fimbriation :tincture-2)
                                   (-> defaults :fimbriation :tincture-2))
        fimbriation-alignment (or (-> line :fimbriation :alignment)
                                  (-> defaults :fimbriation :alignment))
        fimbriation-corner (or (-> line :fimbriation :corner)
                               (-> defaults :fimbriation :corner))
        fimbriation-mode (options/get-value
                          (or (-> line :fimbriation :mode)
                              (-> defaults :fimbriation :mode))
                          (-> options :fimbriation :mode))]
    [:div.setting
     [:label title]
     " "
     [element/submenu path title (get line/line-map line-type) {}
      [form-for-line-type path :options options
       :can-disable? (some? defaults)
       :value line-type
       :default (:type defaults)]
      (when (:eccentricity options)
        [element/range-input-with-checkbox (conj path :eccentricity) "Eccentricity"
         (-> options :eccentricity :min)
         (-> options :eccentricity :max)
         :step 0.01
         :default (or (:eccentricity defaults)
                      (options/get-value line-eccentricity (:eccentricity options)))])
      (when (:height options)
        [element/range-input-with-checkbox (conj path :height) "Height"
         (-> options :height :min)
         (-> options :height :max)
         :step 0.01
         :default (or (:height defaults)
                      (options/get-value line-height (:height options)))])
      (when (:width options)
        [element/range-input-with-checkbox (conj path :width) "Width"
         (-> options :width :min)
         (-> options :width :max)
         :default (or (:width defaults)
                      (options/get-value line-width (:width options)))
         :display-function #(str % "%")])
      (when (:offset options)
        [element/range-input-with-checkbox (conj path :offset) "Offset"
         (-> options :offset :min)
         (-> options :offset :max)
         :step 0.01
         :default (or (:offset defaults)
                      (options/get-value line-offset (:offset options)))])
      (when (-> options :base-line)
        [element/radio-select (conj path :base-line) (-> options :base-line :choices)
         :default (or (:base-line defaults) (-> options :base-line :default))])
      (when (:flipped? options)
        [element/checkbox (conj path :flipped?) "Flipped"])
      (when (:fimbriation options)
        [form-for-fimbriation (conj path :fimbriation) (:fimbriation options)
         {:mode fimbriation-mode
          :alignment fimbriation-alignment
          :tincture-1 fimbriation-tincture-1
          :tincture-2 fimbriation-tincture-2
          :corner fimbriation-corner}
         :defaults {:mode (or (-> defaults :fimbriation :mode)
                              (options/get-value (-> line :fimbriation :mode)
                                                 (-> options :fimbriation :mode)))
                    :alignment (or (-> defaults :fimbriation :alignment)
                                   (options/get-value (-> line :fimbriation :alignment)
                                                      (-> options :fimbriation :alignment)))
                    :thickness-1 (or (-> defaults :fimbriation :thickness-1)
                                     (options/get-value (-> line :fimbriation :thickness-1)
                                                        (-> options :fimbriation :thickness-1)))
                    :thickness-2 (or (-> defaults :fimbriation :thickness-2)
                                     (options/get-value (-> line :fimbriation :thickness-2)
                                                        (-> options :fimbriation :thickness-2)))
                    :corner (or (-> defaults :fimbriation :corner)
                                (options/get-value (-> line :fimbriation :corner)
                                                   (-> options :fimbriation :corner)))}])]]))
