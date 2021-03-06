class ActionsBar extends React.Component {
    formatsDropDown(cb, additionalFormats) {
        return <select onChange={e=>cb(e.target.value, e.target)}>
                <option value="" key="0">Select format</option>
                {[...(additionalFormats||[]), ...AppUtils.formats].map(fmt => <option value={fmt.value} key={fmt.value}>{fmt.displayName}</option>)}
            </select>
    }
    
    getFormat(fmt, selectBox) {
        let fields = this.props.isFiltering ? `&fields=${this.props.fields.join`,`}` : ''
        fmt && window.open(`${location.pathname}?format=${fmt}&override-mimetype=text/plain${fields}`)
        selectBox.selectedIndex = 0
        selectBox.blur()
    }

    getHistory(fmt, selectBox) {
        fmt && window.open(`${location.pathname}?with-history&format=${fmt}&override-mimetype=text/plain`)
        selectBox.selectedIndex = 0
        selectBox.blur()
    }

    render() {
        let formatDropDown = this.props.forInfoton ? <span className="action">
                View in format: 
                {this.formatsDropDown(this.getFormat.bind(this))}
            </span> : null
        
        let historyDropDown = this.props.forInfoton ? <span className="action">
                View History as: 
                {this.formatsDropDown(this.getHistory, [{ value: 'atom', displayName: 'Default (Atom)' }])}
            </span> : null


        let className = `actions-bar${this.props.forInfoton ? " for-infoton" : (this.props.forInfotonsList ? " for-infotons-list" : "")}`
        
        return <div className={className}>
            {formatDropDown}
            {historyDropDown}
        </div>
    }

}

define([], () => ActionsBar)