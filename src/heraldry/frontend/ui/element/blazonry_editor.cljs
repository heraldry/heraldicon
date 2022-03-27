(ns heraldry.frontend.ui.element.blazonry-editor
  (:require
   ["draft-js" :as draft-js]
   ["genex" :as genex]
   ["html-entities" :as html-entities]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.blazonry.parser :as blazonry-parser]
   [heraldry.frontend.auto-complete :as auto-complete]
   [heraldry.frontend.context :as context]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.modal :as modal]
   [heraldry.render :as render]
   [hiccups.runtime]
   [re-frame.core :as rf]
   [reagent.core :as r])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(defn serialize-style [style]
  (->> style
       (map (fn [[k v]]
              (str (name k) ":" v ";")))
       (apply str)))

(defn serialize-styles [data]
  (walk/postwalk (fn [v]
                   (if (and (map? v)
                            (:style v))
                     (assoc v :style (serialize-style (:style v)))
                     v))
                 data))

(defn caret-position [index & {:keys [parent-offset]}]
  (let [parent-offset-top (-> parent-offset :top (or 0))
        parent-offset-left (-> parent-offset :left (or 0))
        selection (js/document.getSelection)
        range-count (.-rangeCount selection)]
    (when (pos? range-count)
      (let [range (.getRangeAt selection 0)
            node (.-startContainer range)
            offset (or (min index (.-length node))
                       (.-startOffset range))]
        (cond
          (pos? offset) (let [rect (-> (doto (js/document.createRange)
                                         (.setStart node (dec offset))
                                         (.setEnd node offset))
                                       .getBoundingClientRect)]
                          {:top (-> rect
                                    .-top
                                    (- parent-offset-top))
                           :left (-> rect
                                     .-left
                                     (- parent-offset-left))})
          (< offset (.-length node)) (let [rect (-> (doto (js/document.createRange)
                                                      (.setStart node offset)
                                                      (.setEnd node (inc offset)))
                                                    .getBoundingClientRect)]
                                       {:top (-> rect
                                                 .-top
                                                 (- parent-offset-top))
                                        :left (-> rect
                                                  .-left
                                                  (- parent-offset-left))})
          :else (let [rect (.getBoundingClientRect node)
                      styles (js/getComputedStyle node)
                      line-height (js/parseInt (.-lineHeight styles))
                      font-size (js/parseInt (.-lineSize styles))
                      delta (/ (- line-height font-size) 2)]
                  {:top (-> rect
                            .-top
                            (+ delta)
                            (- parent-offset-top))
                   :left (-> rect
                             .-left
                             (- parent-offset-left))}))))))

(rf/reg-sub :get-blazon-data
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (try
      (let [hdn (blazonry-parser/blazon->hdn value)]
        {:value value
         :html value
         :hdn hdn})
      (catch :default e
        (let [{:keys [reason index]} (ex-data e)
              parsed (subs value 0 index)
              problem (subs value index)
              auto-complete-choices (->> reason
                                         (mapcat (fn [{:keys [tag expecting]}]
                                                   (case tag
                                                     :optional []
                                                     :string [expecting]
                                                     :regexp (-> expecting
                                                                 genex
                                                                 .generate
                                                                 js->clj))))
                                         vec)]
          {:value value
           :html [:span (html-entities/encode parsed)
                  [:span {:style {:color "red"}} (html-entities/encode problem)]]
           :auto-complete {:choices auto-complete-choices
                           :position (caret-position index)}
           :index index})))))

(defn strip-html-tags [value]
  (-> value
      (s/replace #"<ul.*?</ul>" "")
      (s/replace #"<.*?>" "")))

(macros/reg-event-db :set-blazon-data
  (fn [db [_ path value]]
    (assoc-in db path (-> value
                          strip-html-tags
                          (s/replace "&nbsp;" " ")
                          html-entities/decode))))

(defn on-change [new-editor-state state]
  (reset! state new-editor-state))

(defn core [state]
  (let []
    (r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        [:> draft-js/Editor
                         {:editorState @state
                          :onChange (fn [new-editor-state]
                                      (on-change new-editor-state state))}])})))

(defn blazonry-editor [_html-data _content-path]
  (let [state (r/atom (.createEmpty draft-js/EditorState))]
    (fn [html-data content-path]
      [:div {:style {:display "inline-block"
                     :outline "1px solid black"
                     :width "calc(60% - 10px)"
                     :height "20em"}}
       [core state]])))

(macros/reg-event-db :from-blazon
  (fn [_ [_ context]]
    (modal/create
     :string.button/from-blazon
     [(fn []
        (let [blazon-editor-path [:ui :blazon-editor]
              content-path (conj blazon-editor-path :content)
              hdn-path (conj blazon-editor-path :arms-form)
              {:keys [auto-complete hdn]
               :as data} @(rf/subscribe [:get-blazon-data content-path])]
          (when hdn
            (rf/dispatch [:set hdn-path {:coat-of-arms {:field hdn}
                                         :render-options {:outline? true}}]))
          (auto-complete/set-data auto-complete)
          [:div {:style {:width "40em"
                         :height "20em"}}
           [blazonry-editor
            (-> data :html serialize-styles html)
            content-path]
           [:div {:style {:display "inline-block"
                          :width "40%"
                          :height "100%"
                          :float "right"}}
            [render/achievement
             (assoc
              context/default
              :path hdn-path
              :render-options-path (conj hdn-path :render-options))]]]))])))
