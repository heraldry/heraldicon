(ns heraldry.frontend.ui.element.blazonry-editor
  (:require
   ["draft-js" :as draft-js]
   ["genex" :as genex]
   [clojure.string :as s]
   [heraldry.blazonry.parser :as parser]
   [heraldry.context :as c]
   [heraldry.frontend.auto-complete :as auto-complete]
   [heraldry.frontend.context :as context]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.modal :as modal]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.charge-select :as charge-select]
   [heraldry.interface :as interface]
   [heraldry.render :as render]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def blazon-editor-path
  [:ui :blazon-editor])

(def hdn-path
  (conj blazon-editor-path :arms-form))

(def editor-state-path
  (conj blazon-editor-path :editor-state))

(def parser-path
  (conj blazon-editor-path :parser))

(rf/reg-event-db ::clear-parser
  (fn [db _]
    (assoc-in db parser-path nil)))

(rf/reg-event-db ::update-parser
  (fn [db [_ charges]]
    (assoc-in db parser-path (parser/generate-parser charges))))

(rf/reg-sub :blazonry-parser
  (fn [_ _]
    (rf/subscribe [:get parser-path]))

  (fn [value _]
    (if value
      value
      (do
        (let [update-parser #(rf/dispatch [::update-parser %])
              [status charges] (state/async-fetch-data
                                charge-select/list-db-path
                                :all
                                charge-select/fetch-charge-list
                                :on-success update-parser)]
          (when (= status :done)
            (update-parser charges)))
        parser/default-parser))))

(defn caret-position [index]
  (let [selection (js/document.getSelection)
        range-count (.-rangeCount selection)]
    (when (pos? range-count)
      (let [range (.getRangeAt selection 0)
            node (.-startContainer range)
            node-length (-> node .-length (or 0))
            offset (if index
                     (min index node-length)
                     (.-startOffset range))]
        (cond
          (pos? offset) (let [rect (-> (doto (js/document.createRange)
                                         (.setStart node (dec offset))
                                         (.setEnd node offset))
                                       .getBoundingClientRect)]
                          {:top (.-top
                                 rect)
                           :left (.-left rect)})
          (< offset
             node-length) (let [rect (-> (doto (js/document.createRange)
                                           (.setStart node offset)
                                           (.setEnd node (inc offset)))
                                         .getBoundingClientRect)]
                            {:top (.-top rect)
                             :left (.-left rect)})
          :else (let [node (first (js/document.getElementsByClassName "DraftEditor-root"))
                      rect (.getBoundingClientRect node)
                      styles (js/getComputedStyle node)
                      line-height (js/parseInt (.-lineHeight styles))
                      font-size (js/parseInt (.-fontSize styles))
                      delta (/ (- line-height font-size) 2)]
                  {:top (-> rect
                            .-top
                            (+ delta))
                   :left (.-left rect)}))))))

(def suggestion-hint-order
  (->> ["layout"
        "cottising"
        "line"
        "fimbriation"
        "extra tincture"
        "tincture"
        "ordinary"
        "ordinary option"
        "charge"
        "charge option"
        "partition"
        "attitude"
        "facing"
        "number"]
       (map-indexed (fn [index hint]
                      [hint index]))
       (into {})))

(defn parse-blazonry [value cursor-index parser]
  (try
    (let [hdn (parser/blazon->hdn value parser)]
      {:value value
       :hdn hdn})
    (catch :default e
      (let [{:keys [reason
                    index]} (ex-data e)
            parsed (subs value 0 index)
            problem (subs value index)
            cursor-index (-> cursor-index
                             (max index)
                             (min (count value)))
            typed-string (subs value index cursor-index)
            auto-complete-choices (->> reason
                                       (mapcat (fn [{:keys [tag expecting]}]
                                                 (case tag
                                                   :optional []
                                                   :string [expecting]
                                                   :regexp (-> expecting
                                                               genex
                                                               .generate
                                                               js->clj))))
                                       (map s/trim)
                                       (filter (fn [choice]
                                                 (if choice
                                                   (s/starts-with? choice typed-string)
                                                   true)))
                                       sort
                                       dedupe
                                       (map (fn [choice]
                                              [choice (parser/parse-as-part choice parser)]))
                                       (sort-by (fn [[choice hint]]
                                                  [(get suggestion-hint-order hint 1000)
                                                   choice]))
                                       vec)
            position (caret-position index)]
        {:value value
         :auto-complete (cond-> {:choices auto-complete-choices
                                 :on-click (fn [choice]
                                             (rf/dispatch [:auto-completion-clicked index cursor-index choice]))}
                          position (assoc :position position))
         :index index}))))

(defn get-block-key-and-offset [^draft-js/ContentState content
                                index]
  (loop [[^draft-js/ContentBlock block & rest] (.getBlocksAsArray content)
         index index]
    (when block
      (let [block-length (.getLength block)]
        (if (<= index block-length)
          {:key (.getKey block)
           :offset index}
          (recur rest
                 (- index block-length)))))))

(defn block-start-index [^draft-js/ContentState content block]
  (->> content
       .getBlocksAsArray
       (take-while #(not= (.-key %) (.-key block)))
       (map (fn [^draft-js/ContentBlock block]
              (.getLength block)))
       (reduce +)))

(defn unknown-string-decorator [index]
  (draft-js/CompositeDecorator.
   (clj->js
    [{:strategy (fn [^draft-js/ContentBlock block
                     callback
                     ^draft-js/ContentState content]
                  (when index
                    (let [block-start (block-start-index content block)
                          block-end (+ block-start (.getLength block))]
                      (when (<= index block-end)
                        (callback (-> index
                                      (max block-start)
                                      (- block-start))
                                  (- block-end
                                     block-start))))))
      :component (fn [props]
                   (r/as-element [:span {:style {:color "red"}} (.-children props)]))}])))

(defn cursor-index [^draft-js/EditorState editor-state]
  (let [selection ^draft-js/Selection (.getSelection editor-state)
        content ^draft-js/ContentState (.getCurrentContent editor-state)
        block (->> selection
                   .getFocusKey
                   ^draft-js/ContentBlock (.getBlockForKey content))
        block-start (block-start-index content block)
        offset (.getFocusOffset selection)]
    (+ block-start offset)))

(defn on-editor-change [^draft-js/EditorState new-editor-state]
  (let [content ^draft-js/ContentState (.getCurrentContent new-editor-state)
        text (.getPlainText content)
        cursor-index (cursor-index new-editor-state)
        parser @(rf/subscribe [:blazonry-parser])
        {:keys [hdn
                auto-complete
                index]} (parse-blazonry text cursor-index parser)
        new-editor-state (draft-js/EditorState.set
                          new-editor-state
                          (clj->js
                           {:decorator (unknown-string-decorator index)}))]
    (if auto-complete
      (auto-complete/set-data auto-complete)
      (auto-complete/clear-data))
    (when hdn
      (rf/dispatch [:set (conj hdn-path :coat-of-arms) {:field hdn}]))
    (rf/dispatch [:set editor-state-path new-editor-state])))

(defn put-cursor-at [^draft-js/EditorState state index]
  (let [content (.getCurrentContent state)
        {:keys [key offset]} (get-block-key-and-offset content index)
        selection (-> state
                      ^draft-js/SelectionState (.getSelection)
                      (.merge (clj->js {:anchorKey key
                                        :anchorOffset offset
                                        :focusKey key
                                        :focusOffset offset})))]
    (draft-js/EditorState.forceSelection
     state
     selection)))

(macros/reg-event-db :auto-completion-clicked
  (fn [db [_ index cursor-index choice]]
    (let [state (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent state)
          current-text (.getPlainText content)
          choice (cond-> choice
                   (and (pos? index)
                        (pos? (count current-text))
                        (not= (subs current-text (dec index) index) " ")) (->> (str " "))
                   (and (< cursor-index (count current-text))
                        (not= (subs current-text cursor-index (inc cursor-index)) " ")) (str " "))
          {start-key :key
           start-offset :offset} (get-block-key-and-offset content index)
          {end-key :key
           end-offset :offset} (get-block-key-and-offset content cursor-index)
          range-selection {:anchorKey start-key
                           :anchorOffset start-offset
                           :focusKey end-key
                           :focusOffset end-offset}
          range-selection (-> state
                              ^draft-js/SelectionState (.getSelection)
                              (.merge (clj->js range-selection)))
          new-content (draft-js/Modifier.replaceText
                       content
                       range-selection
                       choice)
          new-state (-> state
                        (draft-js/EditorState.push
                         new-content
                         "insert-characters")
                        (put-cursor-at (-> (count choice)
                                           (+ index))))]
      (on-editor-change new-state)
      (auto-complete/clear-data)
      db)))

(defn blazonry-editor [attributes]
  [:div attributes
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState state
                            :onChange on-editor-change
                            :keyBindingFn (fn [event]
                                            (if (= (.-code event) "Tab")
                                              (do
                                                (.preventDefault event)
                                                (.stopPropagation event)
                                                "auto-complete")
                                              (draft-js/getDefaultKeyBinding event)))
                            :handleKeyCommand (fn [command]
                                                (if (= command "auto-complete")
                                                  (do
                                                    (auto-complete/auto-complete-first)
                                                    "handled")
                                                  "not-handled"))}]))})]])

(macros/reg-event-db :apply-blazon-result
  (fn [db [_ {:keys [path]}]]
    (let [field-data (get-in db (conj hdn-path :coat-of-arms :field))]
      (modal/clear)
      (assoc-in db path field-data))))

(defn open [context]
  (rf/dispatch-sync [:set editor-state-path (.createEmpty draft-js/EditorState)])
  (let [escutcheon (if (->> context
                            :path
                            (take-last 2)
                            (= [:coat-of-arms :field]))
                     (interface/get-sanitized-data (-> context
                                                       (c/-- 2)
                                                       (c/++ :render-options :escutcheon)))
                     :rectangle)]
    (rf/dispatch-sync [:set hdn-path {:coat-of-arms {:field {:type :heraldry.field.type/plain
                                                             :tincture :none}}
                                      :render-options {:escutcheon escutcheon
                                                       :outline? true}}]))
  (modal/create
   [:div
    [tr :string.button/from-blazon]
    [:div.tooltip.info {:style {:display "inline-block"
                                :margin-left "0.2em"}}
     [:sup {:style {:color "#d40"}}
      "alpha"]
     [:div.bottom
      [:p [tr :string.tooltip/alpha-feature-warning]]]]]
   [(fn []
      [:div
       [:div {:style {:display "flex"
                      :flex-flow "row"
                      :height "30em"}}
        [:div.blazonry-editor-help
         {:style {:width "20em"
                  :overflow-y "scroll"}}
         [:p "This blazonry parser is a work in progress. While it already can parse a lot of blazons, there is still a lot of work to do. Once you apply the result you can edit it further in the main interface."]
         [:p "Some things it supports already:"]
         [:ul
          [:li "English blazonry"]
          [:li "TAB auto completes first suggestion"]
          [:li "partitions with sub fields and line styles"
           [:ul
            [:li [:em "per pale indented or and azure"]]
            [:li [:em "tierced per pall gules, argent and or"]]
            [:li [:em "quartered (or, an orle gules), (azure, a fess argent)"]]
            [:li [:em "vairy argent and azure"]]
            [:li [:em "potenty of 6x10 or and gules"]]]]
          [:li "ordinaries with line styles, fimbriation, and cottising"
           [:ul
            [:li [:em "azure, a fess wavy or"]]
            [:li [:em "or, a bend chequy of 12x3 tracts azure and argent"]]
            [:li [:em "azure, a chevronnel enhanced inverted or"]]
            [:li [:em "azure, a pall fimbriated or gules"]]
            [:li [:em "azure, a label of 5 points dovetailed truncated gules"]]
            [:li [:em "azure, a fess or cottised argent and or"]]]]
          [:li "humetty/voided ordinaries"
           [:li [:em "azure, a fess humetty or"]]
           [:li [:em "azure, a pale voided or"]]
           [:li [:em "azure, a pall couped and voided or"]]]
          [:li "ordinary groups"
           [:ul
            [:li [:em "azure, three barrulets or"]]
            [:li [:em "azure, double tressures engrailed or"]]
            [:li [:em "azure, a bordure engrailed or"]]
            [:li [:em "azure, 3 piles throughout or"]]]]
          [:li "charges with fimbriation"
           [:ul
            [:li [:em "azure, a star of 6 points fimbriated or sable"]]
            [:li [:em "azure, a lion or, langued gules, armed argent"]]
            [:li [:em "azure, a lion sejant reversed or"]]]]
          [:li "charge groups"
           [:ul
            [:li [:em "azure, a chief argent charged with 3 stars sable"]]
            [:li [:em "per pale gules, or, twelve stars counterchanged in annullo"]]
            [:li [:em "azure, 10 roundels or 4 3 2 1"]]
            [:li [:em "azure, 8 stars sable in orle"]]]]]
         [:p "Some things that still need work or have known issues:"
          [:ul
           [:li "blazonry in other languages"]
           [:li "explicit charge positioning, e.g. 'in chief', 'in base'"]
           [:li "charge/ordinary arrangement in relation to each other, e.g. 'between'"]
           [:li "semy"]
           [:li "partition field referencing by number or location, e.g. 'i. and iv. ...' or 'in sinister ...'"]
           [:li "previous tincture referencing, e.g. 'of the first', 'of the field'"]]]]
        [:div {:style {:width "20em"
                       :height "100%"
                       :margin-left "10px"
                       :margin-right "10px"}}
         [blazonry-editor
          {:style {:display "inline-block"
                   :outline "1px solid black"
                   :width "100%"
                   :height "100%"}}]]
        [:div {:style {:width "15em"
                       :height "100%"}}
         [render/achievement
          (assoc
           context/default
           :path hdn-path
           :render-options-path (conj hdn-path :render-options))]]]

       [:div.buttons {:style {:display "flex"}}
        [:div {:style {:flex "auto"}}]
        [:button.button
         {:type "button"
          :style {:flex "initial"
                  :margin-left "10px"}
          :on-click (fn [_]
                      (auto-complete/clear-data)
                      (modal/clear))}
         [tr :string.button/cancel]]

        [:button.button.primary {:type "submit"
                                 :on-click #(rf/dispatch [:apply-blazon-result context])
                                 :style {:flex "initial"
                                         :margin-left "10px"}}
         [tr :string.button/apply]]]])]
   :on-cancel #(auto-complete/clear-data)))
